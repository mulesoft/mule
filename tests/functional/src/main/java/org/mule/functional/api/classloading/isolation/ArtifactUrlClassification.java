/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static org.mule.runtime.core.util.Preconditions.checkNotNull;

import java.net.URL;
import java.util.List;

/**
 * Defines the result of the classification process for a {@link org.mule.runtime.module.artifact.Artifact}. It contains a
 * {@link List} of {@link URL}s for its {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}.
 *
 * @since 4.0
 */
public class ArtifactUrlClassification {

  private final List<URL> urls;
  private final String name;

  /**
   * Creates an instance of the classification.
   *
   * @param name a {@link String} representing the name of the artifact. Not null.
   * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}. Not null.
   */
  public ArtifactUrlClassification(String name, List<URL> urls) {
    checkNotNull(name, "name cannot be null");
    checkNotNull(urls, "urls cannot be null");

    this.name = name;
    this.urls = urls;
  }

  public List<URL> getUrls() {
    return urls;
  }

  public String getName() {
    return name;
  }

}
