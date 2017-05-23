/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classification;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.test.runner.api.WorkspaceLocationResolver;
import org.mule.test.runner.maven.MavenModelFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link WorkspaceReader} that resolves artifacts using the IDE workspace or Maven multi-module reactor.
 *
 * @since 4.0
 */
public class DefaultWorkspaceReader implements WorkspaceReader {

  private static final String WORKSPACE = "workspace";

  private static final String MAVEN_SHADE_PLUGIN_ARTIFACT_ID = "maven-shade-plugin";
  private static final String ORG_APACHE_MAVEN_PLUGINS_GROUP_ID = "org.apache.maven.plugins";
  private static final String REDUCED_POM_XML = "dependency-reduced-pom.xml";

  private static final String POM = "pom";
  private static final String POM_XML = POM + ".xml";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final WorkspaceRepository workspaceRepository = new WorkspaceRepository(WORKSPACE);
  private final WorkspaceLocationResolver workspaceLocationResolver;
  private final List<URL> classPath;

  /**
   * Creates and instance of the workspace reader for the given classPath and workspaceLocationResolver.
   *
   * @param classPath {@link URL}s to find the artifact's {@link URL}
   * @param workspaceLocationResolver {@link WorkspaceLocationResolver} to retrieve the workspace reference for a given
   *        {@link Artifact}
   */
  public DefaultWorkspaceReader(List<URL> classPath, WorkspaceLocationResolver workspaceLocationResolver) {
    checkNotNull(classPath, "classPath cannot be null");
    checkNotNull(workspaceLocationResolver, "workspaceLocationResolver cannot be null");

    this.classPath = classPath;
    this.workspaceLocationResolver = workspaceLocationResolver;
  }

  /**
   * Looks for a matching {@link URL} for a workspace {@link Artifact}. It also supports to look for jars or classes depending if
   * the artifacts were packaged or not.
   *
   * @param artifact to be used in order to find the {@link URL} in list of urls
   * @param classPath a list of {@link URL} obtained from the classPath
   * @return {@link File} that represents the {@link Artifact} passed or null
   */
  public static File findClassPathURL(final Artifact artifact, final File workspaceArtifactPath, final List<URL> classPath) {
    final Path moduleFolder = new File(workspaceArtifactPath.getAbsoluteFile(), "target").toPath();

    // Fix to handle when running test during an install phase due to maven builds the classPath pointing out to packaged files
    // instead of classes folders.
    final StringBuilder explodedUrlSuffix = new StringBuilder();
    final StringBuilder packagedUrlSuffix = new StringBuilder();
    if (isTestArtifact(artifact)) {
      explodedUrlSuffix.append("test-classes");
      packagedUrlSuffix.append(".*-tests.jar");
    } else {
      explodedUrlSuffix.append("classes");
      packagedUrlSuffix.append("^(?!.*?(?:-tests.jar)).*.jar");
    }
    final Optional<URL> localFile = classPath.stream().filter(url -> {
      Path path = toFile(url).toPath();
      if (path.startsWith(moduleFolder)) {
        String file = path.getFileName().toFile().getName();
        return file.matches(explodedUrlSuffix.toString()) || file.matches(packagedUrlSuffix.toString());
      }
      return false;
    }).findFirst();
    if (!localFile.isPresent()) {
      return null;
    }
    return toFile(localFile.get());
  }

  /**
   * Determines whether the specified artifact refers to test classes.
   *
   * @param artifact The artifact to check, must not be {@code null}.
   * @return {@code true} if the artifact refers to test classes, {@code false} otherwise.
   */
  public static boolean isTestArtifact(Artifact artifact) {
    return ("test-jar".equals(artifact.getProperty("type", "")))
        || ("jar".equals(artifact.getExtension()) && "tests".equals(artifact.getClassifier()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkspaceRepository getRepository() {
    return workspaceRepository;
  }

  @Override
  public File findArtifact(Artifact artifact) {
    File workspaceArtifactPath = workspaceLocationResolver.resolvePath(artifact.getArtifactId());
    if (workspaceArtifactPath == null) {
      logger.debug("Couldn't resolve '{}' from workspace, it would be resolved against local repository or downloaded", artifact);
      // Cannot be resolved in workspace so delegate its resolution to the Maven local repository
      return null;
    }

    File artifactFile;
    if (artifact.getExtension().equals(POM)) {
      artifactFile = findPomFile(artifact, workspaceArtifactPath);
    } else {
      artifactFile = findClassPathURL(artifact, workspaceArtifactPath, classPath);
    }

    if (artifactFile != null && artifactFile.exists()) {
      logger.debug("Artifact '{}' resolved from workspace {}", artifact, artifactFile.getAbsolutePath());
      return artifactFile.getAbsoluteFile();
    }
    return null;
  }

  /**
   * Not need to specify the versions here.
   *
   * @param artifact to look for its versions
   * @return an empty {@link List}
   */
  @Override
  public List<String> findVersions(Artifact artifact) {
    return findArtifact(artifact) == null ? emptyList() : singletonList(artifact.getVersion());
  }

  /**
   * Resolves the location of the {@value #POM_XML} {@link File} taking into account {@value #MAVEN_SHADE_PLUGIN_ARTIFACT_ID}
   * plugin.
   *
   * @param artifact {@link Artifact} to get its {@value #POM_XML}
   * @param workspacePath {@link File} referencing the location of the {@link Artifact} in the workspace
   * @return {@link File} to the {@value #POM_XML} of the artifact from the workspace path
   */
  private File findPomFile(Artifact artifact, File workspacePath) {
    Plugin shadeMavenPlugin =
        searchForMavenShadePlugin(MavenModelFactory.createMavenProject(new File(workspacePath, POM_XML)));
    if (shadeMavenPlugin != null) {
      // TODO (gfernandes) MULE-10485 - add support for reading the plugin configuration using Xpp3 Maven API
      // MavenXpp3Reader.parsePluginConfiguration(...)
      File reducedPom = new File(workspacePath, REDUCED_POM_XML);
      if (!reducedPom.exists()) {
        throw new IllegalStateException(artifact + " has in its build configure the " + shadeMavenPlugin + " but default "
            + REDUCED_POM_XML
            + " is not present. Run the plugin first.");
      }
      logger.debug("Using {} for artifact {}", reducedPom, artifact);
      return reducedPom;
    } else {
      return new File(workspacePath, POM_XML);
    }
  }

  /**
   * Searches the Maven {@link Model} for the {@url https://maven.apache.org/plugins/maven-shade-plugin/}. maven-shared-plugin
   * would have to be configure in the pom directly and not inherited from a parent pom due to it is not supported by this
   * implementation.
   *
   * @param model Maven {@link Model} to look for the Maven Shade Plugin
   * @return {@link Plugin} model for Maven Shade Plugin if found.
   */
  private Plugin searchForMavenShadePlugin(Model model) {
    if (model.getBuild() != null) {
      for (Plugin plugin : model.getBuild().getPlugins()) {
        if (plugin.getGroupId().equals(ORG_APACHE_MAVEN_PLUGINS_GROUP_ID)
            && plugin.getArtifactId().equals(MAVEN_SHADE_PLUGIN_ARTIFACT_ID)) {
          return plugin;
        }
      }
    }

    return null;
  }
}
