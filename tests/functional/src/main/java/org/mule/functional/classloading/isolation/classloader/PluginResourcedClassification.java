/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classloader;

import org.mule.functional.api.classloading.isolation.PluginUrlClassification;

import java.util.Set;

/**
 * {@link PluginUrlClassification} along with its resources and packages exported.
 *
 * @since 4.0
 */
public class PluginResourcedClassification {

  private PluginUrlClassification pluginUrlClassification;
  private Set<String> exportedPackages;
  private Set<String> exportedResources;

  public PluginResourcedClassification(PluginUrlClassification pluginUrlClassification, Set<String> exportedPackages,
                                       Set<String> exportedResources) {
    this.pluginUrlClassification = pluginUrlClassification;
    this.exportedPackages = exportedPackages;
    this.exportedResources = exportedResources;
  }

  public PluginUrlClassification getPluginUrlClassification() {
    return pluginUrlClassification;
  }

  public Set<String> getExportedResources() {
    return exportedResources;
  }

  public Set<String> getExportedPackages() {
    return exportedPackages;
  }

}
