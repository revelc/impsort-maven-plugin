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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.google.common.hash.Hashing;

import net.revelc.code.impsort.Grouper;
import net.revelc.code.impsort.ImpSort;
import net.revelc.code.impsort.LineEnding;
import net.revelc.code.impsort.Result;

abstract class AbstractImpSortMojo extends AbstractMojo {

  private static final String[] DEFAULT_INCLUDES = new String[] {"**/*.java"};

  /** The Constant CACHE_PROPERTIES_FILENAME. */
  private static final String CACHE_PROPERTIES_FILENAME = "impsort-maven-cache.properties";

  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  @Parameter(defaultValue = "${plugin}", readonly = true)
  protected PluginDescriptor plugin;

  @Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true)
  protected String sourceEncoding = StandardCharsets.UTF_8.name();

  /**
   * Allows skipping execution of this plugin.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "skip", property = "impsort.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Configures the grouping of static imports. Groups are defined with comma-separated package name
   * prefixes. The special "*" group refers to imports not matching any other group, and is implied
   * after all other groups, if not specified. More specific groups are prioritized over less
   * specific ones. All groups are sorted.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "staticGroups", property = "impsort.staticGroups", defaultValue = "*")
  protected String staticGroups;

  /**
   * Configures the grouping of non-static imports. Groups are defined with comma-separated package
   * name prefixes. The special "*" group refers to imports not matching any other group, and is
   * implied after all other groups, if not specified. More specific groups are prioritized over
   * less specific ones. All groups are sorted.
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
  @Parameter(alias = "joinStaticWithNonStatic", property = "impsort.joinStaticWithNonStatic",
      defaultValue = "false")
  protected boolean joinStaticWithNonStatic;

  /**
   * Project's main source directory as specified in the POM. Used by default if
   * <code>directories</code> is not set.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "sourceDirectory", defaultValue = "${project.build.sourceDirectory}",
      readonly = true)
  private File sourceDirectory;

  /**
   * Project's test source directory as specified in the POM. Used by default if
   * <code>directories</code> is not set.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "testSourceDirectory", defaultValue = "${project.build.testSourceDirectory}",
      readonly = true)
  private File testSourceDirectory;

  /**
   * Project's base directory.
   */
  @Parameter(defaultValue = ".", property = "project.basedir", readonly = true, required = true)
  private File basedir;

  /**
   * Location of the Java source files to process. Defaults to source main and test directories if
   * not set.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "directories", property = "impsort.directories")
  private File[] directories;

  /**
   * List of fileset patterns for Java source locations to include. Patterns are relative to the
   * directories selected. When not specified, the default include is <code>**&#47;*.java</code>
   *
   * @since 1.0.0
   */
  @Parameter(alias = "includes", property = "impsort.includes")
  private String[] includes;

  /**
   * List of fileset patterns for Java source locations to exclude. Patterns are relative to the
   * directories selected. When not specified, there is no default exclude.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "excludes", property = "impsort.excludes")
  private String[] excludes;

  /**
   * Configures whether to remove unused imports.
   *
   * @since 1.1.0
   */
  @Parameter(alias = "removeUnused", property = "impsort.removeUnused", defaultValue = "false")
  private boolean removeUnused;

  /**
   * Configures whether to treat imports in the current package as unused and subject to removal
   * along with other unused imports.
   *
   * @since 1.2.0
   */
  @Parameter(alias = "treatSamePackageAsUnused", property = "impsort.treatSamePackageAsUnused",
      defaultValue = "true")
  private boolean treatSamePackageAsUnused;

  /**
   * Configures whether to use a breadth first comparator for sorting static imports. This will
   * ensure all static imports from one class are grouped together before any static imports from an
   * inner-class.
   *
   * @since 1.3.0
   */
  @Parameter(alias = "breadthFirstComparator", property = "impsort.breadthFirstComparator",
      defaultValue = "true")
  private boolean breadthFirstComparator;

  /**
   * Sets the line-ending of files after formatting. Valid values are:
   * <ul>
   * <li><b>"AUTO"</b> - Use line endings of current system</li>
   * <li><b>"KEEP"</b> - Preserve line endings of files, default to AUTO if ambiguous</li>
   * <li><b>"LF"</b> - Use Unix and Mac style line endings</li>
   * <li><b>"CRLF"</b> - Use DOS and Windows style line endings</li>
   * <li><b>"CR"</b> - Use early Mac style line endings</li>
   * </ul>
   *
   * @since 1.4.0
   */
  @Parameter(alias = "lineEnding", property = "impsort.lineEnding", defaultValue = "AUTO")
  private LineEnding lineEnding;

  /**
   * Sets the Java source compliance level (e.g. 1.0, 1.5, 1.7, 8, 9, 11, etc.)
   *
   * @since 1.5.0
   */
  @Parameter(alias = "compliance", property = "impsort.compliance",
      defaultValue = "${maven.compiler.release}")
  private String compliance;

  /**
   * Projects cache directory.
   *
   * <p>
   * This file is a hash cache of the files in the project source. It can be preserved in source
   * code such that it ensures builds are always fast by not unnecessarily writing files constantly.
   * It can also be added to gitignore in case startup is not necessary. It further can be
   * redirected to another location.
   *
   * <p>
   * When stored in the repository, the cache if run on cross platforms will display the files
   * multiple times due to line ending differences on the platform.
   *
   * @since 1.6.0
   */
  @Parameter(defaultValue = "${project.build.directory}", property = "impsort.cachedir")
  private File cachedir;

  abstract byte[] processResult(Path path, Result results) throws MojoFailureException;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping execution of impsort-maven-plugin");
      return;
    }

    // find all matching files
    Stream<File> files;
    if (directories != null && directories.length > 0) {
      // warn if a user-specified directory doesn't exist
      files = Stream.of(directories).flatMap(d -> searchDir(d, true)).parallel();
    } else {
      // default to src/main/java and src/test/java, without existence warnings
      files = Stream.of(sourceDirectory, testSourceDirectory).flatMap(d -> searchDir(d, false))
          .parallel();
    }
    Stream<Path> paths = files.map(File::toPath);
    Properties hashCache = readFileHashCacheFile();
    AtomicBoolean hashCacheModified = new AtomicBoolean();
    Path baseDir = this.basedir.toPath();

    // process all found files, and aggregate any failures
    Grouper grouper = new Grouper(groups, staticGroups, staticAfter, joinStaticWithNonStatic,
        breadthFirstComparator);
    Charset encoding = Charset.forName(sourceEncoding);

    LanguageLevel langLevel = getLanguageLevel(compliance);
    getLog().info("Using compiler compliance level: " + langLevel.toString());
    ImpSort impSort = new ImpSort(encoding, grouper, removeUnused, treatSamePackageAsUnused,
        lineEnding, langLevel);
    AtomicLong numAlreadySorted = new AtomicLong(0);
    AtomicLong numProcessed = new AtomicLong(0);

    Function<Path, MojoFailureException> visitor = path -> {
      try {
        getLog().debug("Reading file " + path);

        try {
          byte[] buf = Files.readAllBytes(path);
          String newHash = getHash(buf);
          String key = baseDir.relativize(path).toString();
          String prvHash = hashCache.getProperty(key);
          if (prvHash != null && prvHash.equals(newHash)) {
            numAlreadySorted.getAndIncrement();
            getLog().debug("Unchanged: " + path);
          } else {
            Result result = impSort.parseFile(path, buf);
            result.getImports().forEach(imp -> getLog().debug("Found import: " + imp));
            if (result.isSorted()) {
              numAlreadySorted.getAndIncrement();
            } else {
              numProcessed.getAndIncrement();
            }
            buf = processResult(path, result);
            if (buf != null) {
              newHash = getHash(buf);
            }
            hashCache.setProperty(key, newHash);
            hashCacheModified.set(true);
          }
        } catch (IOException e) {
          fail("Error reading file " + path, e);
        }
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

    long startTime = System.nanoTime();
    MojoFailureException failure = paths.map(visitor).filter(notNull).reduce(agg).orElse(null);
    Duration totalTime = Duration.ofNanos(System.nanoTime() - startTime);

    long total = numAlreadySorted.get() + numProcessed.get();
    long minutes = totalTime.getSeconds() / 60;
    long seconds = totalTime.getSeconds() - minutes * 60;
    long millis = totalTime.getNano() / 1_000_000;
    String fmt = "%22s: %" + Long.toString(total).length() + "d";
    getLog().info(String.format(fmt + " in %02d:%02d.%03d", "Total Files Processed", total, minutes,
        seconds, millis));
    getLog().info(String.format(fmt, "Already Sorted", numAlreadySorted.get()));
    getLog().info(String.format(fmt, "Needed Sorting", numProcessed.get()));

    // check for failures during processing
    if (failure != null) {
      throw failure;
    }
    if (hashCacheModified.get()) {
      storeFileHashCache(hashCache);
    }
  }

  private String getHash(byte[] buf) {
    return Hashing.murmur3_128().hashBytes(buf).toString();
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
    return Stream.of(ds.getIncludedFiles()).map(filename -> new File(dir, filename)).parallel();
  }

  protected void fail(String message) throws MojoFailureException {
    fail(message, null);
  }

  protected void fail(String message, Throwable cause) throws MojoFailureException {
    throw cause == null ? new MojoFailureException(message)
        : new MojoFailureException(message, cause);
  }

  static LanguageLevel getLanguageLevel(String compliance) {
    if (compliance == null || compliance.trim().isEmpty()) {
      return LanguageLevel.POPULAR;
    }
    String langLevel = "";
    String v = compliance.trim();
    if (v.matches("^1[.][01234]$")) {
      langLevel = "JAVA_" + v.replace(".", "_");
    } else if (v.matches("^1[.][56789]$")) {
      langLevel = "JAVA_" + v.replaceFirst("^.*[.]", "");
    } else {
      langLevel = "JAVA_" + v;
    }
    return LanguageLevel.valueOf(langLevel);
  }

  /**
   * Store file hash cache.
   *
   * @param props the props
   */
  private void storeFileHashCache(Properties props) {
    File cacheFile = new File(this.cachedir, CACHE_PROPERTIES_FILENAME);
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(cacheFile))) {
      props.store(out, null);
    } catch (IOException e) {
      getLog().warn("Cannot store file hash cache properties file", e);
    }
  }

  /**
   * Read file hash cache file.
   *
   * @return the properties
   */
  private Properties readFileHashCacheFile() {
    Properties props = new Properties();
    Log log = getLog();
    if (!this.cachedir.exists()) {
      if (!this.cachedir.mkdirs()) {
        log.warn("Unable to create cache directory '" + this.cachedir.getPath() + "'.");
      }
    } else if (!this.cachedir.isDirectory()) {
      log.warn("Something strange here as the '" + this.cachedir.getPath()
          + "' supposedly cache directory is not a directory.");
      return props;
    }

    File cacheFile = new File(this.cachedir, CACHE_PROPERTIES_FILENAME);
    if (!cacheFile.exists()) {
      return props;
    }

    try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(cacheFile))) {
      props.load(stream);
    } catch (IOException e) {
      log.warn("Cannot load file hash cache properties file", e);
    }
    return props;
  }

}
