/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.infrastructure.maven;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

  private static final String M_2_REPO = "/.m2/repository";
  private static final String USER_HOME = "user.home";
  private static final URL MAVEN_ARTIFACTS_DIRECTORY =
      MavenTestUtils.class.getClassLoader().getResource("artifacts");

  private static final List<String> INSTALL_GOALS = Collections.singletonList("install");
  private static final List<String> CLEAN_GOALS = Collections.singletonList("clean");

  private static final InvokerLogger LOGGER = new SystemOutLogger();

  private static final File MAVEN_SETTINGS = new File(getProperty("settings.file"));

  private MavenTestUtils() {}

  /**
   * Runs the Maven install goal using the project on the given directory for the artifact defined by the given descriptor. After
   * the artifact has been installed, performs the Maven clean goal to delete the intermediate resources.
   * 
   * @param baseDirectory directory on which the POM resides.
   * @param descriptor the artifact descriptor for the project being built.
   * @return the installed artifact on the Maven repository.
   */
  public static File installMavenArtifact(String baseDirectory, BundleDescriptor descriptor) {
    runMavenGoal(INSTALL_GOALS, baseDirectory);
    runMavenGoal(CLEAN_GOALS, baseDirectory);
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
        .get(descriptor.getGroupId(), descriptor.getArtifactId(), descriptor.getVersion(),
             descriptor.getArtifactFileName() + "." + descriptor.getType())
        .toString());

    if (!artifact.exists()) {
      throw new IllegalArgumentException(format("Maven artifact %s does not exists in the local Maven repository", descriptor));
    }

    return artifact;
  }

  private static File getMavenLocalRepository() {
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

  private static void runMavenGoal(List<String> goals, String baseDirectory) {
    Invoker invoker = new DefaultInvoker();
    invoker.setLocalRepositoryDirectory(getMavenLocalRepository());
    invoker.setLogger(LOGGER);
    LOGGER.setThreshold(3);
    InvocationRequest request = new DefaultInvocationRequest();
    request.setGoals(goals);
    request.setBatchMode(true);

    String mavenArtifactsAndBaseDirectory = MAVEN_ARTIFACTS_DIRECTORY.toString() + "/" + baseDirectory;
    request.setBaseDirectory(Paths.get(URI.create(mavenArtifactsAndBaseDirectory)).toFile());
    request.setPomFile(Paths.get(URI.create(mavenArtifactsAndBaseDirectory + "/pom.xml")).toFile());
    request.setShowErrors(true);
    request.setUserSettingsFile(MAVEN_SETTINGS);
    try {
      InvocationResult result = invoker.execute(request);
      if (result.getExitCode() != 0) {
        LOGGER.error(result.getExecutionException().getMessage());
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
