/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import java.net.URL;
import java.util.List;

/**
 * Defines the list of URLS for each class loader that would be created in order to run the test. It is the result of
 * {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ArtifactsUrlClassification {

  private final List<URL> containerUrls;
  private final List<ArtifactUrlClassification> serviceUrlClassifications;
  private final List<URL> pluginSharedLibUrls;
  private final List<PluginUrlClassification> pluginUrlClassifications;
  private final List<URL> applicationUrls;

  /**
   * Creates a instance with the list of {@link URL}s classified in container, plugins and application.
   *
   * @param containerUrls list of {@link URL} that define the artifacts that would be loaded with the container
   *        {@link ClassLoader}. Not null.
   * @param serviceUrlClassifications for each plugin discovered a list of {@link ArtifactUrlClassification} that defines the
   *        artifact that would be loaded by the service {@link ClassLoader}. Not null.
   * @param pluginSharedLibUrls of {@link URL} that define the artifacts that would be loaded with the shareLib
   *        {@link ClassLoader}
   * @param pluginUrlClassifications for each plugin discovered a list of {@link PluginUrlClassification} that defines the
   *        artifact that would be loaded by the plugin {@link ClassLoader}. Not null.
   * @param applicationUrls list of {@link URL} that define the artifacts that would be loaded with the application
   *        {@link ClassLoader}. Not null.
   */
  public ArtifactsUrlClassification(List<URL> containerUrls, List<ArtifactUrlClassification> serviceUrlClassifications,
                                    List<URL> pluginSharedLibUrls, List<PluginUrlClassification> pluginUrlClassifications,
                                    List<URL> applicationUrls) {
    checkNotNull(containerUrls, "containerUrls cannot be null");
    checkNotNull(serviceUrlClassifications, "serviceUrlClassifications cannot be null");
    checkNotNull(pluginSharedLibUrls, "pluginSharedLibUrls cannot be null");
    checkNotNull(pluginUrlClassifications, "pluginUrlClassifications cannot be null");
    checkNotNull(applicationUrls, "applicationUrls cannot be null");

    this.containerUrls = containerUrls;
    this.serviceUrlClassifications = serviceUrlClassifications;
    this.pluginSharedLibUrls = pluginSharedLibUrls;
    this.pluginUrlClassifications = pluginUrlClassifications;
    this.applicationUrls = applicationUrls;
  }

  public List<URL> getContainerUrls() {
    return containerUrls;
  }

  public List<ArtifactUrlClassification> getServiceUrlClassifications() {
    return serviceUrlClassifications;
  }

  public List<URL> getPluginSharedLibUrls() {
    return pluginSharedLibUrls;
  }

  public List<PluginUrlClassification> getPluginUrlClassifications() {
    return pluginUrlClassifications;
  }

  public List<URL> getApplicationUrls() {
    return applicationUrls;
  }
}
