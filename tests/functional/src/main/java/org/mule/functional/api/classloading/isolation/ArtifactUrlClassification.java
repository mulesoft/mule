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
 * Defines the list of URLS for each class loader that would be created in order to run the test. It is the result of
 * {@link ClassPathClassifier}.
 *
 * @since 4.0
 */
public class ArtifactUrlClassification {

  private final List<URL> containerUrls;
  private final List<PluginUrlClassification> pluginClassificationsUrls;
  private final List<URL> applicationUrls;

  /**
   * Creates a instance with the list of {@link URL}s classified in container, plugins and application.
   *
   * @param containerUrls list of {@link URL} that define the artifacts that would be loaded with the container
   *        {@link ClassLoader}
   * @param pluginClassificationsUrls for each plugin discovered a list of {@link URL} that define the artifacts that would be
   *        loaded by the plugin {@link ClassLoader}
   * @param applicationUrls list of {@link URL} that define the artifacts that would be loaded with the application
   *        {@link ClassLoader}
   */
  public ArtifactUrlClassification(List<URL> containerUrls, List<PluginUrlClassification> pluginClassificationsUrls,
                                   List<URL> applicationUrls) {
    this.containerUrls = containerUrls;
    this.pluginClassificationsUrls = pluginClassificationsUrls;
    this.applicationUrls = applicationUrls;
  }

  public List<URL> getContainerUrls() {
    return containerUrls;
  }

  public List<PluginUrlClassification> getPluginClassificationUrls() {
    return pluginClassificationsUrls;
  }

  public List<URL> getApplicationUrls() {
    return applicationUrls;
  }
}
