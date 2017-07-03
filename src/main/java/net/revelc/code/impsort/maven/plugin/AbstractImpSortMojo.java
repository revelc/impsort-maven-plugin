/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.revelc.code.impsort.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

abstract class AbstractImpSortMojo extends AbstractMojo {

  private static final String[] DEFAULT_INCLUDES = new String[] {"**/*.java"};

  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  @Parameter(defaultValue = "${plugin}", readonly = true)
  protected PluginDescriptor plugin;

  /**
   * Allows skipping execution of this plugin.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "skip", property = "impsort.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Configures the grouping of static imports. Groups are defined with comma-separated package name prefixes. The special "*" group refers to imports not
   * matching any other group, and is implied after all other groups, if not specified. More specific groups are prioritized over less specific ones. All groups
   * are sorted.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "staticGroups", property = "impsort.staticGroups", defaultValue = "*")
  protected String staticGroups;

  /**
   * Configures the grouping of non-static imports. Groups are defined with comma-separated package name prefixes. The special "*" group refers to imports not
   * matching any other group, and is implied after all other groups, if not specified. More specific groups are prioritized over less specific ones. All groups
   * are sorted.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "groups", property = "impsort.groups", defaultValue = "*")
  protected String groups;

  /**
   * Configures whether static groups will appear after non-static groups.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "staticAfter", property = "impsort.staticAfter", defaultValue = "false")
  protected boolean staticAfter;

  /**
   * Allows omitting the blank line between the static and non-static sections.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "joinStaticWithNonStatic", property = "impsort.joinStaticWithNonStatic", defaultValue = "false")
  protected boolean joinStaticWithNonStatic;

  /**
   * Project's source directory as specified in the POM.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "sourceDirectory", property = "impsort.sourceDirectory", defaultValue = "${project.build.sourceDirectory}")
  private File sourceDirectory;

  /**
   * Project's test source directory as specified in the POM.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "testSourceDirectory", property = "impsort.testSourceDirectory", defaultValue = "${project.build.testSourceDirectory}")
  private File testSourceDirectory;

  /**
   * Location of the Java source files to process. Defaults to source main and test directories if not set.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "directories", property = "impsort.directories")
  private File[] directories;

  /**
   * List of fileset patterns for Java source locations to include. Patterns are relative to the directories selected. When not specified, the default include
   * is <code>**&#47;*.java</code>
   *
   * @since 1.0.0
   */
  @Parameter(alias = "includes", property = "impsort.includes")
  private String[] includes;

  /**
   * List of fileset patterns for Java source locations to exclude. Patterns are relative to the directories selected. When not specified, there is no default
   * exclude.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "includes", property = "impsort.excludes")
  private String[] excludes;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping execution of impsort-maven-plugin");
      return;
    }

    findMatchingFiles().forEach(System.out::println);
    impSortExec();
  }

  private List<File> findMatchingFiles() {
    List<File> files = new ArrayList<>();
    if (directories != null && directories.length > 0) {
      for (File directory : directories) {
        if (directory.exists() && directory.isDirectory()) {
          files.addAll(addCollectionFiles(directory));
        } else {
          getLog().warn("Directory does not exist or is not a directory: " + directory);
        }
      }
    } else { // Using defaults of source main and test dirs
      if (sourceDirectory != null && sourceDirectory.exists() && sourceDirectory.isDirectory()) {
        files.addAll(addCollectionFiles(sourceDirectory));
      }
      if (testSourceDirectory != null && testSourceDirectory.exists() && testSourceDirectory.isDirectory()) {
        files.addAll(addCollectionFiles(testSourceDirectory));
      }
    }

    return files;
  }

  private List<File> addCollectionFiles(File newBasedir) {
    getLog().debug("Adding directory " + newBasedir);
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(newBasedir);
    if (includes != null && includes.length > 0) {
      ds.setIncludes(includes);
    } else {
      ds.setIncludes(DEFAULT_INCLUDES);
    }

    ds.setExcludes(excludes);
    ds.addDefaultExcludes();
    ds.setCaseSensitive(false);
    ds.setFollowSymlinks(false);
    ds.scan();

    List<File> foundFiles = new ArrayList<>();
    for (String filename : ds.getIncludedFiles()) {
      foundFiles.add(new File(newBasedir, filename));
    }
    return foundFiles;
  }

  abstract void impSortExec() throws MojoExecutionException, MojoFailureException;
}
