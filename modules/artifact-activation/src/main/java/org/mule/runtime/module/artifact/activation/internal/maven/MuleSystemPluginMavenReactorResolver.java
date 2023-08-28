/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static java.lang.Math.random;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import static org.apache.commons.io.FileUtils.deleteQuietly;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.maven.pom.parser.api.model.MavenPomModel;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Resolve the maven pom file for the given artifact.
 *
 * @since 4.5.0
 */
public class MuleSystemPluginMavenReactorResolver implements MavenReactorResolver, AutoCloseable {

  private static final String POM = "pom";

  private final File temporaryFolder;

  private final MavenPomModel effectiveModel;
  private final File pomFile;
  private final File artifactFile;

  public MuleSystemPluginMavenReactorResolver(File artifactFile, MavenClient mavenClient) {
    try {
      temporaryFolder = createTempDirectory("tmpDirPrefix" + random()).toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.effectiveModel = mavenClient.getEffectiveModel(artifactFile, of(temporaryFolder));

    this.pomFile = effectiveModel.getPomFile().get();
    this.artifactFile = artifactFile;
  }

  @Override
  public File findArtifact(BundleDescriptor bundleDescriptor) {
    if (checkArtifact(bundleDescriptor)) {
      if (bundleDescriptor.getType().equals(POM)) {
        return pomFile;
      } else {
        return artifactFile;
      }
    }
    return null;
  }

  @Override
  public List<String> findVersions(BundleDescriptor bundleDescriptor) {
    if (checkArtifact(bundleDescriptor)) {
      return singletonList(this.effectiveModel.getVersion());
    }
    return emptyList();
  }

  private boolean checkArtifact(BundleDescriptor bundleDescriptor) {
    return this.effectiveModel.getGroupId().equals(bundleDescriptor.getGroupId())
        && this.effectiveModel.getArtifactId().equals(bundleDescriptor.getArtifactId())
        && this.effectiveModel.getVersion().equals(bundleDescriptor.getVersion());
  }

  @Override
  public void close() {
    deleteQuietly(temporaryFolder);
  }
}
