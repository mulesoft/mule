/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;

import static org.apache.commons.io.FileUtils.toFile;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

/**
 * Helper class for dynamically resolving dependencies to be used for the tests.
 */
public class DependencyResolver {

  /**
   * Resolves the specified artifact from the given GAV coordinates.
   *
   * @param groupId    Group ID for the artifact.
   * @param artifactId Artifact ID.
   * @param version    Version of the artifact.
   * @return The URL where the resolved artifact can be located.
   */
  public static URL getDependencyFromMaven(String groupId, String artifactId, String version) {
    ClassLoader classLoader = DependencyResolver.class.getClassLoader();
    URL settingsUrl = classLoader.getResource("custom-settings.xml");
    MavenClientProvider mavenClientProvider = discoverProvider(classLoader);

    Supplier<File> localMavenRepository =
        mavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier();

    MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder =
        newMavenConfigurationBuilder().globalSettingsLocation(toFile(settingsUrl));

    BundleDescriptor bundleDescriptor = new BundleDescriptor.Builder()
        .setGroupId(groupId)
        .setArtifactId(artifactId)
        .setVersion(version)
        .build();

    try (MavenClient mavenClient = mavenClientProvider
        .createMavenClient(mavenConfigurationBuilder.localMavenRepositoryLocation(localMavenRepository.get()).build())) {
      return mavenClient.resolveBundleDescriptor(bundleDescriptor).getBundleUri().toURL();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
