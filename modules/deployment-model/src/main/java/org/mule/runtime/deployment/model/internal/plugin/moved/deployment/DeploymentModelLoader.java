/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment;


import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.moved.Plugin;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedDeploymentModelException;
import org.mule.runtime.deployment.model.internal.plugin.moved.deployment.descriptor.MavenClassloaderDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.moved.deployment.descriptor.PropertiesClassloaderDescriptor;


/**
 * Given a {@link Plugin}, it will look for the concrete strategies for loading a {@link DeploymentModel}.
 * This class is the intended one to
 *
 * @since 4.0
 */
public class DeploymentModelLoader {

  /**
   * Takes a well formed {@link Plugin} and looks in the current implementations that could take care of that specific plugin
   *
   * @param plugin the plugin to introspect
   * @return a {@link DeploymentModel} if there's a strategy for the plugin's class
   * @throws MalformedDeploymentModelException when there's an error in the plugin's descriptor, etc.
   * @throws IllegalArgumentException if the location of the plugin doesn't match any current implementation (local for zip and folders only)
     */
  public static DeploymentModel from(Plugin plugin) throws MalformedDeploymentModelException {

    String id = plugin.getPluginDescriptor().getClassloaderModelDescriptor().getId();
    //TODO MULE-10785 should be replaced by SPI rather than hitting the classes directly
    if (id.equals(MavenClassloaderDescriptor.MAVEN)) {
      return new MavenClassloaderDescriptor().load(plugin.getLocation(),
                                                   plugin.getPluginDescriptor().getClassloaderModelDescriptor().getAttributes());
    } else if (id.equals(PropertiesClassloaderDescriptor.PLUGINPROPERTIES)) {
      return new PropertiesClassloaderDescriptor()
          .load(plugin.getLocation(), plugin.getPluginDescriptor().getClassloaderModelDescriptor().getAttributes());
    } else {
      throw new IllegalArgumentException(format("There's no support for %s type on the current classloaders descriptors support",
                                                id));
    }
  }
}
