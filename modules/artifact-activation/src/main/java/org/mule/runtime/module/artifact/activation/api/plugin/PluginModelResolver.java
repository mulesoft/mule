/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

/**
 * Resolves the {@link MulePluginModel} by deserializing it from the {@code mule-artifact.json} within the jar of a plugin.
 *
 * @since 4.5
 */
public interface PluginModelResolver {

  /**
   * @param bundleDescriptor the bundle descriptor of the plugin to get the artifact descriptor for.
   */
  MulePluginModel resolve(BundleDescriptor bundleDescriptor);

}
