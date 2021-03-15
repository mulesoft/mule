/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.maven;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.regex.Matcher.quoteReplacement;
import static org.apache.maven.shared.invoker.InvokerLogger.INFO;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutLogger;

/**
 * Provides Maven related utilities for testing purposes
 */
public class MavenTestUtils {

  private static final InvokerLogger LOGGER = new SystemOutLogger();

  private static final String M_2_REPO = "/.m2/repository";
  private static final String USER_HOME = "user.home";
  private static final URI MAVEN_ARTIFACTS_DIRECTORY = ofNullable(getProperty("mule.test.maven.artifacts.dir"))
      .map(dir -> new File(dir).toURI())
      .orElseGet(() -> {
        try {
          return MavenTestUtils.class.getClassLoader().getResource("artifacts").toURI();
        } catch (URISyntaxException e) {
          LOGGER.error("Could not resolve default Maven artifacts directory", e);
          return null;
        }
      });

  private static final List<String> INSTALL_GOALS = singletonList("install");
  private static final List<String> CLEAN_GOALS = singletonList("clean");

  private static final File MAVEN_SETTINGS = new File(getProperty("settings.file"));

  private static final Invoker INVOKER;

  static {
    INVOKER = new DefaultInvoker();
    INVOKER.setLocalRepositoryDirectory(getMavenLocalRepository());
    INVOKER.setLogger(LOGGER);
    LOGGER.setThreshold(INFO);
  }

  private MavenTestUtils() {}

  /**
   * Runs the Maven install goal using the project on the given directory for the artifact defined by the given descriptor. After
   * the artifact has been installed, performs the Maven clean goal to delete the intermediate resources.
   *
   * @param baseDirectory directory on which the POM resides.
   * @param descriptor    the artifact descriptor for the project being built.
   * @return the installed artifact on the Maven repository.
   */
  public static File installMavenArtifact(String baseDirectory, BundleDescriptor descriptor) {
    return installMavenArtifact(baseDirectory, descriptor, new Properties());
  }

  /**
   * Runs the Maven install goal using the project on the given directory for the artifact defined by the given descriptor. After
   * the artifact has been installed, performs the Maven clean goal to delete the intermediate resources.
   *
   * @param baseDirectory directory on which the POM resides.
   * @param descriptor    the artifact descriptor for the project being built.
   * @param props         the system properties to pass the Maven jobs
   * @return the installed artifact on the Maven repository.
   */
  public static File installMavenArtifact(String baseDirectory, BundleDescriptor descriptor, Properties props) {
    runMavenGoal(INSTALL_GOALS, baseDirectory, props);
    runMavenGoal(CLEAN_GOALS, baseDirectory, props);
    return findMavenArtifact(descriptor);
  }

  /**
   * Obtains the file corresponding to a given Maven artifact on the local repo
   *
   * @param descriptor describes which artifact must be returned.
   * @return the file corresponding to the given artifact
   * @throws IllegalArgumentException if the file does not exists in the local Maven repository
   */
  public static File findMavenArtifact(BundleDescriptor descriptor) {
    File artifact = new File(getMavenLocalRepository(), Paths
        .get(descriptor.getGroupId().replaceAll("\\.", quoteReplacement(separator)),
             descriptor.getArtifactId(),
             descriptor.getVersion(),
             descriptor.getArtifactFileName() + "." + descriptor.getType())
        .toString());

    if (!artifact.exists()) {
      LOGGER.error(format("Artifact file: %s", artifact.getAbsolutePath()));
      throw new IllegalArgumentException(format("Maven artifact '%s' does not exists in the local Maven repository @ '%s'",
                                                descriptor, getMavenLocalRepository()));
    }

    return artifact;
  }

  public static File getMavenLocalRepository() {
    String buildDirectory = getProperty("localRepository");
    if (buildDirectory == null) {
      buildDirectory = getProperty(USER_HOME) + M_2_REPO;
    }

    File mavenLocalRepositoryLocation = new File(buildDirectory);
    if (!mavenLocalRepositoryLocation.exists()) {
      throw new IllegalArgumentException("Maven repository location couldn't be found, please check your configuration");
    }
    return mavenLocalRepositoryLocation;
  }

  private static void runMavenGoal(List<String> goals, String baseDirectory, Properties props) {
    InvocationRequest request = new DefaultInvocationRequest();
    request.setGoals(goals);
    request.setBatchMode(true);
    request.setProperties(props);
    // avoid JVM optimizations for short-lived jvms running maven builds
    request.setMavenOpts("-XX:+TieredCompilation -XX:TieredStopAtLevel=1");
    File mavenArtifactsAndBaseDirectory;
    File baseDirFile = new File(baseDirectory);
    if (baseDirFile.isAbsolute()) {
      mavenArtifactsAndBaseDirectory = baseDirFile;
    } else {
      mavenArtifactsAndBaseDirectory = new File(new File(MAVEN_ARTIFACTS_DIRECTORY), baseDirectory);
    }

    LOGGER.info("Using Maven artifacts base directory: '" + mavenArtifactsAndBaseDirectory.getAbsolutePath() + "'...");

    request.setBaseDirectory(mavenArtifactsAndBaseDirectory);
    request.setPomFile(new File(mavenArtifactsAndBaseDirectory, "pom.xml"));
    request.setShowErrors(true);
    request.setUserSettingsFile(MAVEN_SETTINGS);
    try {
      InvocationResult result = INVOKER.execute(request);
      if (result.getExitCode() != 0) {
        LOGGER.error("Error while running Maven invoker", result.getExecutionException());
        throw new RuntimeException("Error while running Maven invoker", result.getExecutionException());
      }

    } catch (MavenInvocationException e) {
      throw new RuntimeException("Error running Maven project: " + e.getMessage());
    }
  }

  public static BundleDescriptor getApplicationBundleDescriptor(String appName, Optional<String> classifier) {
    return new BundleDescriptor.Builder().setGroupId("test").setArtifactId(appName)
        .setVersion("1.0.0").setClassifier(classifier.orElse("mule-application")).build();
  }

  public static BundleDescriptor getDomainBundleDescriptor(String appName) {
    return new BundleDescriptor.Builder().setGroupId("test").setArtifactId(appName)
        .setVersion("1.0.0").setClassifier("mule-domain").build();
  }
}
