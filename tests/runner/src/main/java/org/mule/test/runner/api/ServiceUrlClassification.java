/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.api;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;

import java.net.URL;
import java.util.List;

/**
 * Defines the result of the classification process for a {@link Service}. It contains a {@link ServiceDescriptor}, {@link List}
 * of {@link URL}s for its {@link ArtifactClassLoader}.
 *
 * @since 4.5
 */
public class ServiceUrlClassification {

  private final ServiceDescriptor descriptor;
  private final List<URL> urls;
  private final String name;
  private final String artifactId;

  /**
   * Creates an instance of the classification.
   *
   * @param name a {@link String} representing the name of the artifact. Not null.
   * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}. Not null.
   */
  public ServiceUrlClassification(ServiceDescriptor descriptor, String artifactId, String name, List<URL> urls) {
    requireNonNull(descriptor, "descriptor cannot be null");
    requireNonNull(artifactId, "artifactId cannot be null");
    requireNonNull(name, "name cannot be null");
    requireNonNull(urls, "urls cannot be null");

    this.descriptor = descriptor;
    this.artifactId = artifactId;
    this.name = name;
    this.urls = urls;
  }

  public ServiceDescriptor getDercriptor() {
    return descriptor;
  }

  public List<URL> getUrls() {
    return urls;
  }

  public String getName() {
    return name;
  }

  public String getArtifactId() {
    return artifactId;
  }
}
