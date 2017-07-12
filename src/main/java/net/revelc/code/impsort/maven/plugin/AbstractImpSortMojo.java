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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
   * Project's main source directory as specified in the POM. Used by default if <code>directories</code> is not set.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "sourceDirectory", defaultValue = "${project.build.sourceDirectory}", readonly = true)
  private File sourceDirectory;

  /**
   * Project's test source directory as specified in the POM. Used by default if <code>directories</code> is not set.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "testSourceDirectory", defaultValue = "${project.build.testSourceDirectory}", readonly = true)
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
  @Parameter(alias = "excludes", property = "impsort.excludes")
  private String[] excludes;

  abstract void processFile(File f) throws MojoFailureException;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping execution of impsort-maven-plugin");
      return;
    }

    // find all matching files
    Stream<File> files = null;
    if (directories != null && directories.length > 0) {
      // warn if a user-specified directory doesn't exist
      files = Stream.of(directories).flatMap(d -> searchDir(d, true));
    } else {
      // default to src/main/java and src/test/java, without existence warnings
      files = Stream.of(sourceDirectory, testSourceDirectory).flatMap(d -> searchDir(d, false));
    }

    // process all found files, and aggregate any failures
    Function<File,MojoFailureException> visitor = f -> {
      try {
        processFile(f);
        return null;
      } catch (MojoFailureException e) {
        return e;
      }
    };
    Predicate<MojoFailureException> notNull = e -> e != null;
    BinaryOperator<MojoFailureException> agg = (e1, e2) -> {
      e1.addSuppressed(e2);
      return e1;
    };
    MojoFailureException failure = files.map(visitor).filter(notNull).reduce(agg).orElse(null);

    // check for failures during processing
    if (failure != null) {
      throw failure;
    }
  }

  private Stream<File> searchDir(File dir, boolean warnOnBadDir) {
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
      if (warnOnBadDir && dir != null) {
        getLog().warn("Directory does not exist or is not a directory: " + dir);
      }
      return Stream.empty();
    }
    getLog().debug("Adding directory " + dir);
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(dir);
    ds.setIncludes(includes != null && includes.length > 0 ? includes : DEFAULT_INCLUDES);
    ds.setExcludes(excludes);
    ds.addDefaultExcludes();
    ds.setCaseSensitive(false);
    ds.setFollowSymlinks(false);
    ds.scan();
    return Stream.of(ds.getIncludedFiles()).map(filename -> new File(dir, filename));
  }

  protected void fail(String message) throws MojoFailureException {
    fail(message, null);
  }

  protected void fail(String message, Throwable cause) throws MojoFailureException {
    throw cause == null ? new MojoFailureException(message) : new MojoFailureException(message, cause);
  }

}
