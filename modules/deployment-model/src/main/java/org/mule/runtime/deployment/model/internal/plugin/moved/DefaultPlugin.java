/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved;

import org.mule.runtime.deployment.model.api.plugin.moved.Plugin;
import org.mule.runtime.deployment.model.api.plugin.moved.PluginDescriptor;

import java.net.URL;

/**
 * Default implementation of {@link Plugin}
 *
 * @since 4.0
 */
public class DefaultPlugin implements Plugin {

  private PluginDescriptor pluginDescriptor;
  private URL location;

  public DefaultPlugin(PluginDescriptor pluginDescriptor, URL location) {
    this.pluginDescriptor = pluginDescriptor;
    this.location = location;
  }

  @Override
  public PluginDescriptor getPluginDescriptor() {
    return pluginDescriptor;
  }

  @Override
  public URL getLocation() {
    return location;
  }
}
