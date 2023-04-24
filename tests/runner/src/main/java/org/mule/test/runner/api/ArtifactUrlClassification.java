/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.net.URL;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;

/**
 * Defines the result of the classification process for an {@link Artifact}. It contains a {@link List} of {@link URL}s for its
 * {@link ArtifactClassLoader}.
 *
 * @since 4.0
 */
public class ArtifactUrlClassification {

  private final List<URL> urls;
  private final Artifact artifact;
  private final String name;
  private final String artifactId;

  /**
   * Creates an instance of the classification.
   *
   * @param artifactId a {@link String} representing the id of the artifact. Not null.
   * @param artifact the Maven artifact representation. Not null.
   * @param urls     list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}. Not null.
   */
  public ArtifactUrlClassification(String artifactId, Artifact artifact, List<URL> urls) {
    requireNonNull(artifactId, "artifactId cannot be null");
    requireNonNull(artifact, "artifact cannot be null");
    requireNonNull(urls, "urls cannot be null");

    this.artifactId = artifactId;
    this.artifact = artifact;
    this.name = artifact.getArtifactId();
    this.urls = urls;
  }

  public List<URL> getUrls() {
    return urls;
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public String getName() {
    return name;
  }

  public String getArtifactId() {
    return artifactId;
  }
}
