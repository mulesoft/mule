/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static java.util.Collections.emptySet;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Defines the result of the classification process for a plugin. It contains a {@link List} of {@link URL}s that should have the
 * plugin {@link ArtifactClassLoader} plus a {@link List} of {@link Class}es to be
 * exported in addition to the packages exported by the plugin, in order to run the test.
 *
 * @since 4.0
 */
public class PluginUrlClassification {

  private static final String INVALID_PRIVILEGED_API_DEFINITION_ERROR =
      "Invalid privileged API: both privileged packages and artifacts must be defined";

  private final List<URL> urls;
  private final String name;
  private final List<Class> exportClasses;
  private final List<String> pluginDependencies;
  private final Set<String> exportedPackages;
  private final Set<String> exportedResources;
  private final Set<String> privilegedExportedPackages;
  private final Set<String> privilegedArtifacts;

  /**
   * Creates an instance of the classification.
   * @param name a {@link String} representing the name of the plugin
   * @param urls list of {@link URL}s that would be used to create the {@link java.net.URLClassLoader}
   * @param exportClasses list of {@link Class}es that would be used for exporting as extra classes to the plugin.
  *                      Can be {@code null}.
   * @param pluginDependencies list of {@link String} plugin dependencies names for this plugin classified
   * @param exportedPackages {@link Set} of exported packages by this plugin
   * @param exportedResources {@link Set} of exported resources by this plugin
   * @param privilegedExportedPackages* @param privilegedExportedPackages java packages exported by this module to privileged artifacts only
   * @param privilegedArtifacts name of the artifacts with privileged access to the API.
   */
  public PluginUrlClassification(String name, List<URL> urls, List<Class> exportClasses, List<String> pluginDependencies,
                                 Set<String> exportedPackages, Set<String> exportedResources,
                                 Set<String> privilegedExportedPackages, Set<String> privilegedArtifacts) {
    checkNotNull(name, "name cannot be null");
    checkNotNull(urls, "urls cannot be null");
    checkNotNull(pluginDependencies, "pluginDependencies cannot be null");
    checkNotNull(exportedPackages, "exportedPackages cannot be null");
    checkNotNull(exportedResources, "exportedResources cannot be null");

    checkArgument(privilegedExportedPackages != null, "privilegedExportedPackages cannot be null");
    checkArgument(privilegedArtifacts != null, "privilegedArtifacts cannot be null");
    checkArgument((privilegedArtifacts.isEmpty() && privilegedExportedPackages.isEmpty())
        || (!privilegedArtifacts.isEmpty() && !privilegedExportedPackages.isEmpty()), INVALID_PRIVILEGED_API_DEFINITION_ERROR);
    this.name = name;
    this.urls = urls;
    this.exportClasses = exportClasses;
    this.pluginDependencies = pluginDependencies;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
    this.privilegedExportedPackages = privilegedExportedPackages;
    this.privilegedArtifacts = privilegedArtifacts;
  }

  public PluginUrlClassification(String name, List<URL> urls, List<Class> exportClasses, List<String> pluginDependencies) {
    this(name, urls, exportClasses, pluginDependencies, emptySet(), emptySet(), emptySet(), emptySet());
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

  public Set<String> getPrivilegedExportedPackages() {
    return privilegedExportedPackages;
  }

  public Set<String> getPrivilegedArtifacts() {
    return privilegedArtifacts;
  }
}
