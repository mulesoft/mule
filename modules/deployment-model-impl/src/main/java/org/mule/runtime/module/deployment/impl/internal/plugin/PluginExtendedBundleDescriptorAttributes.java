/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.util.Objects.requireNonNull;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.Map;

import org.apache.commons.collections.map.AbstractMapDecorator;

/**
 * Allows to extends the attributes defined for a {@link org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor}
 * when it is being loaded by {@link org.mule.runtime.module.deployment.impl.internal.maven.MavenBundleDescriptorLoader} for plugins in
 * order to use the effective {@link BundleDescriptor} resolved from the artifact that declares the plugin. This allows to
 * work with SNAPSHOT versions in order to propagate the timestamped version of the SNAPSHOT artifact.
 *
 * @since 4.2.0
 */
public class PluginExtendedBundleDescriptorAttributes extends AbstractMapDecorator {

  private final BundleDescriptor pluginBundleDescriptor;

  public PluginExtendedBundleDescriptorAttributes(Map<String, Object> attributes, BundleDescriptor pluginBundleDescriptor) {
    super(attributes);
    requireNonNull(pluginBundleDescriptor, "pluginBundleDescriptor cannot be null");
    this.pluginBundleDescriptor = pluginBundleDescriptor;
  }

  public BundleDescriptor getPluginBundleDescriptor() {
    return pluginBundleDescriptor;
  }
}
