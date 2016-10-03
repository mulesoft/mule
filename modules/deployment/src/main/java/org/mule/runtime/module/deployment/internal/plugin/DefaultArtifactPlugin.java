/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;

import java.io.File;

/**
 * Defines an application plugin artifact
 */
public class DefaultArtifactPlugin implements ArtifactPlugin {

  private final ArtifactPluginDescriptor descriptor;
  private final ArtifactClassLoader classLoader;

  /**
   * Creates a new plugin
   *
   * @param descriptor describes the plugin to create. Non null.
   * @param classLoader classloader to use on this plugin. Non null.
   */
  public DefaultArtifactPlugin(ArtifactPluginDescriptor descriptor, ArtifactClassLoader classLoader) {
    this.descriptor = descriptor;
    this.classLoader = classLoader;
  }

  @Override
  public ArtifactPluginDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public String getArtifactName() {
    return descriptor.getName();
  }

  @Override
  public File[] getResourceFiles() {
    return new File[0];
  }

  @Override
  public ArtifactClassLoader getArtifactClassLoader() {
    return classLoader;
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), getArtifactName(), Integer.toHexString(System.identityHashCode(this)));
  }
}
