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
 * Defines the result of the classification process for a plugin. It contains a {@link List} of {@link URL}s that should have the
 * plugin {@link org.mule.runtime.module.artifact.classloader.ArtifactClassLoader} plus a {@link List} of {@link Class}es to be
 * exported in addition to the packages exported by the plugin, in order to run the test.
 *
 * @since 4.0
 */
public class PluginUrlClassification {

  private final List<URL> urls;
  private final String name;
  private final List<Class> exportClasses;

  /**
   * Creates an instance of the classification.
   *
   * @param name a {@link String} representing the name of the plugin
   * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}
   * @param exportClasses list of {@link Class}es that would be used for exporting as extra classes to the plugin
   */
  public PluginUrlClassification(String name, List<URL> urls, List<Class> exportClasses) {
    this.name = name;
    this.urls = urls;
    this.exportClasses = exportClasses;
  }

  public List<URL> getUrls() {
    return urls;
  }

  public String getName() {
    return name;
  }

  public List<Class> getExportClasses() {
    return exportClasses;
  }
}
