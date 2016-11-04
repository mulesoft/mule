/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.classloadermodel;


import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.loader.Plugin;
import org.mule.runtime.deployment.model.api.plugin.PluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.ClassloaderModel;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;
import org.mule.runtime.deployment.model.internal.plugin.descriptor.MavenClassloaderDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.descriptor.PropertiesClassloaderDescriptor;

import java.net.URL;


/**
 * Given a {@link Plugin}, it will look for the concrete strategies for loading a {@link ClassloaderModel}.
 * This class is the intended one to
 *
 * @since 4.0
 */
public class ClassloaderModelLoader {

  /**
   * Takes a well formed {@link Plugin} and looks in the current implementations that could take care of that specific plugin
   *
   * @param plugin the plugin to introspect
   * @return a {@link ClassloaderModel} if there's a strategy for the plugin's class
   * @throws MalformedClassloaderModelException when there's an error in the plugin's classloadermodel, etc.
   * @throws IllegalArgumentException if the location of the plugin doesn't match any current implementation (local for zip and folders only)
     */
  public static ClassloaderModel from(Plugin plugin) throws MalformedClassloaderModelException {

    PluginDescriptor pluginDescriptor = plugin.getPluginDescriptor();
    URL location = plugin.getLocation();
    return from(location, pluginDescriptor);
  }

  public static ClassloaderModel from(URL pluginLocation, PluginDescriptor pluginDescriptor)
      throws MalformedClassloaderModelException {
    String id = pluginDescriptor.getClassloaderModelDescriptor().getId();
    //TODO MULE-10876 should be replaced by SPI rather than hitting the classes directly
    if (id.equals(MavenClassloaderDescriptor.MAVEN)) {
      return new MavenClassloaderDescriptor().load(pluginLocation,
                                                   pluginDescriptor.getClassloaderModelDescriptor().getAttributes());
    } else if (id.equals(PropertiesClassloaderDescriptor.PLUGINPROPERTIES)) {
      return new PropertiesClassloaderDescriptor()
          .load(pluginLocation, pluginDescriptor.getClassloaderModelDescriptor().getAttributes());
    } else {
      throw new IllegalArgumentException(format("There's no support for %s type on the current classloaders descriptors support",
                                                id));
    }
  }
}
