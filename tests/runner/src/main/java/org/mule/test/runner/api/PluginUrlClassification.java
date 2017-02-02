/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
  private final List<String> pluginDependencies;
  private Set<String> exportedPackages;
  private Set<String> exportedResources;

  /**
   * Creates an instance of the classification.
   *
   * @param name a {@link String} representing the name of the plugin
   * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}
   * @param exportClasses list of {@link Class}es that would be used for exporting as extra classes to the plugin.
   *                      Can be {@code null}.
   * @param pluginDependencies list of {@link String} plugin dependencies names for this plugin classified
   * @param exportedPackages {@link Set} of exported packages by this plugin
   * @param exportedResources {@link Set} of exported resources by this plugin
   */
  public PluginUrlClassification(String name, List<URL> urls, List<Class> exportClasses, List<String> pluginDependencies,
                                 Set<String> exportedPackages, Set<String> exportedResources) {
    checkNotNull(name, "name cannot be null");
    checkNotNull(urls, "urls cannot be null");
    checkNotNull(pluginDependencies, "pluginDependencies cannot be null");
    checkNotNull(exportedPackages, "exportedPackages cannot be null");
    checkNotNull(exportedResources, "exportedResources cannot be null");

    this.name = name;
    this.urls = urls;
    this.exportClasses = exportClasses;
    this.pluginDependencies = pluginDependencies;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
  }

  public PluginUrlClassification(String name, List<URL> urls, List<Class> exportClasses, List<String> pluginDependencies) {
    this(name, urls, exportClasses, pluginDependencies, Collections.<String>emptySet(), Collections.<String>emptySet());
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

  public List<String> getPluginDependencies() {
    return pluginDependencies;
  }

  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

  public Set<String> getExportedResources() {
    return exportedResources;
  }

  public String getArtifactId() {
    return this.getName();
  }
}
