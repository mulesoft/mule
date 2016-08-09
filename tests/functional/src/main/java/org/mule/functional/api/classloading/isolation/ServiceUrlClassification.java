/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import java.net.URL;
import java.util.List;

/**
 * Defines the result of the classification process for a {@link org.mule.runtime.api.service.Service}. It contains a {@link List}
 * of {@link URL}s that should have the service {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader}.
 *
 * @since 4.0
 */
public class ServiceUrlClassification {

  private final List<URL> urls;
  private final String name;

  /**
   * Creates an instance of the classification.
   *
   * @param name a {@link String} representing the name of the plugin
   * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}
   */
  public ServiceUrlClassification(String name, List<URL> urls) {
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
