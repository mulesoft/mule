/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.maven;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.getProperty;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.Paths.get;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.core.api.util.StringMessageUtils.getBoilerPlate;
import org.mule.test.runner.api.WorkspaceLocationResolver;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers Maven projects from the rootArtifactClassesFolder folder and Maven variable
 * {@value #MAVEN_MULTI_MODULE_PROJECT_DIRECTORY} (if present) to define the root project directory.
 * <p/>
 * Matches each Maven project found with the class path in order to check if it is part of the build session. When
 * {@value #MAVEN_MULTI_MODULE_PROJECT_DIRECTORY} is not present, meaning that this not running under a Maven build session,
 * Workspace references are resolved by filtering class path {@link URL}s if they reference to a Maven project (a target/classes
 * or target/test-classes/ folders).
 * <p/>
 * If Maven surefire plugin is used to run test in Maven and the plugin has been configured with {@code forkMode=always} the
 * following Maven system property has to be propagated on surefire configuration:
 * 
 * <pre>
 *  <systemPropertyVariables>
 *    <maven.multiModuleProjectDirectory>${maven.multiModuleProjectDirectory}</maven.multiModuleProjectDirectory>
 *  </systemPropertyVariables>
 * </pre>
 * <p/>
 *
 * @since 4.0
 */
public class AutoDiscoverWorkspaceLocationResolver implements WorkspaceLocationResolver {

  public static final String POM_XML_FILE = "pom.xml";
  public static final String MAVEN_MULTI_MODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Map<String, File> filePathByArtifactId = new HashMap<>();

  /**
   * Creates an instance of this class.
   *
   * @param classPath {@link URL}'s defined in class path
   * @throws IllegalArgumentException if the rootArtifactClassesFolder doesn't point to a Maven project.
   */
  public AutoDiscoverWorkspaceLocationResolver(List<URL> classPath, File rootArtifactClassesFolder) {
    checkNotNull(classPath, "classPath cannot be null");
    checkNotNull(rootArtifactClassesFolder, "rootArtifactClassesFolder cannot be null");

    File rootArtifactFolder = rootArtifactClassesFolder.getParentFile().getParentFile();
    logger.debug("Discovering workspace artifacts locations from '{}'", rootArtifactFolder);
    if (!containsMavenProject(rootArtifactFolder)) {
      logger.warn("Couldn't find any workspace reference for artifacts due to '{}' is not a Maven project", rootArtifactFolder);
    }

    String rootProjectDirectoryProperty = getProperty(MAVEN_MULTI_MODULE_PROJECT_DIRECTORY);
    if (isNotBlank(rootProjectDirectoryProperty)) {
      logger.debug("Using Maven System.property['{}']='{}' to find out project root directory for discovering poms",
                   MAVEN_MULTI_MODULE_PROJECT_DIRECTORY, rootProjectDirectoryProperty);
      discoverMavenReactorProjects(rootProjectDirectoryProperty, classPath,
                                   rootArtifactClassesFolder.getParentFile().getParentFile());
    } else {
      logger.debug("Filtering class path entries to find out Maven projects");
      discoverMavenProjectsFromClassPath(classPath);
    }

    logger.debug("Workspace location discover process completed");

    List<String> messages = newArrayList("Workspace:");
    messages.add(" ");
    messages.addAll(filePathByArtifactId.entrySet().stream().map(entry -> entry.getKey() + " -> (" + (entry.getValue()) + ")")
        .collect(toList()));
    logger.info(getBoilerPlate(newArrayList(messages), '*', 150));
  }

  /**
   * Traverses the directory tree from the {@value #MAVEN_MULTI_MODULE_PROJECT_DIRECTORY} property to look for Maven projects that
   * are also listed as entries in class path.
   *
   * @param rootProjectDirectoryProperty the root directory of the multi-module project build session
   * @param classPath the whole class path built by IDE or Maven (surefire Maven plugin)
   * @param rootArtifactClassesFolder the current rootArtifact directory
   */
  private void discoverMavenReactorProjects(String rootProjectDirectoryProperty, List<URL> classPath,
                                            File rootArtifactClassesFolder) {
    Path rootProjectDirectory = get(rootProjectDirectoryProperty);
    logger.debug("Defined rootProjectDirectory='{}'", rootProjectDirectory);

    File currentDir = rootArtifactClassesFolder;
    File lastMavenProjectDir = currentDir;
    while (containsMavenProject(currentDir) && !currentDir.toPath().equals(rootProjectDirectory.getParent())) {
      lastMavenProjectDir = currentDir;
      currentDir = currentDir.getParentFile();
    }

    logger.debug("Top folder found, parent pom found at: '{}'", lastMavenProjectDir);
    try {
      walkFileTree(lastMavenProjectDir.toPath(), new MavenDiscovererFileVisitor(classPath));
    } catch (IOException e) {
      throw new RuntimeException("Error while discovering Maven projects from path: " + currentDir.toPath());
    }
  }

  /**
   * Discovers Maven projects by searching in class path provided by IDE or Maven (surefire Maven plugin) by looking at those
   * {@link URL}s that have a {@value #POM_XML_FILE} in its {@code url.toFile.getParent.getParent}, because reference between
   * modules in IDE should be like the following:
   * 
   * <pre>
   *    /Users/jdoe/Development/mule/extensions/file/target/test-classes
   *    /Users/jdoe/Development/mule/extensions/file/target/classes
   *    /Users/jdoe/Development/mule/core/target/classes
   * </pre>
   * 
   * @param classPath
   */
  private void discoverMavenProjectsFromClassPath(List<URL> classPath) {
    List<Path> classPaths = classPath.stream().map(url -> toFile(url).toPath()).collect(toList());
    List<File> mavenProjects = classPaths.stream()
        .filter(path -> containsMavenProject(path.getParent().getParent().toFile()))
        .map(path -> path.getParent().getParent().toFile()).collect(toList());
    logger.debug("Filtered from class path Maven projects: {}", mavenProjects);
    mavenProjects.stream().forEach(file -> resolvedArtifact(readMavenPomFile(getPomFile(file)).getArtifactId(), file.toPath()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File resolvePath(String artifactId) {
    return filePathByArtifactId.get(artifactId);
  }

  /**
   * Reads the Maven pom file to get build the {@link Model}.
   *
   * @param pomFile to be read
   * @return {@link Model} represeting the Maven project
   */
  private Model readMavenPomFile(File pomFile) {
    MavenXpp3Reader mavenReader = new MavenXpp3Reader();

    try (FileReader reader = new FileReader(pomFile)) {
      return mavenReader.read(reader);
    } catch (Exception e) {
      throw new RuntimeException("Error while reading Maven model from " + pomFile, e);
    }
  }

  /**
   * Creates a {@link File} for the {@value #POM_XML_FILE} in the given directory
   *
   * @param currentDir to create a {@value #POM_XML_FILE}
   * @return {@link File} to the {@value #POM_XML_FILE} in the give directory
   */
  private File getPomFile(File currentDir) {
    return new File(currentDir, POM_XML_FILE);
  }

  /**
   * @param dir {@link File} directory to check if it has a {@value #POM_XML_FILE}
   * @return true if the directory contains a {@value #POM_XML_FILE}
   */
  private boolean containsMavenProject(File dir) {
    return dir.isDirectory() && getPomFile(dir).exists();
  }

  /**
   * @param file {@link File} to check if it a {@value #POM_XML_FILE}
   * @return true if the file is a {@value #POM_XML_FILE}
   */
  private boolean isPomFile(File file) {
    return file.getName().equalsIgnoreCase(POM_XML_FILE);
  }

  /**
   * Adds the resolved artifact with its path.
   *
   * @param artifactId the Maven artifactId found in workspace
   * @param path the {@link Path} location to the artifactId
   */
  private void resolvedArtifact(String artifactId, Path path) {
    logger.trace("Resolved artifactId from workspace at {}={}", artifactId, path);
    filePathByArtifactId.put(artifactId, path.toFile());
  }

  /**
   * Looks for directories that contain a {@value #POM_XML_FILE} file so it will be added to the resolved artifacts locations.
   */
  private class MavenDiscovererFileVisitor implements FileVisitor<Path> {

    private List<Path> classPath;

    public MavenDiscovererFileVisitor(List<URL> urlClassPath) {
      this.classPath =
          urlClassPath.stream().map(url -> toFile(url).getParentFile().getParentFile().toPath()).collect(toList());
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      return getPomFile(dir.toFile()).exists() ? CONTINUE : SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      if (isPomFile(file.toFile())) {
        Model model = readMavenPomFile(file.toFile());
        Path location = file.getParent();
        logger.debug("Checking if location {} is already present in class path", location);
        if (this.classPath.contains(location)) {
          resolvedArtifact(model.getArtifactId(), location);
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      return CONTINUE;
    }
  }

}
