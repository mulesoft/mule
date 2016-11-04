/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.loader;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.api.plugin.PluginDescriptor;
import org.mule.runtime.deployment.model.internal.plugin.descriptor.DefaultPlugin;
import org.mule.runtime.deployment.model.internal.plugin.loader.FolderPluginDescriptorLoader;
import org.mule.runtime.deployment.model.internal.plugin.loader.ZipPluginDescriptorLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Marker interface to load {@link PluginDescriptor} from several sources (ZIP, a folder)
 * TODO MULE-10875 this class should become the new {@link org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin}
 * @since 4.0
 */
public interface Plugin {

  /**
   * @return the classloadermodel of the current plugin. Non null.
   */
  PluginDescriptor getPluginDescriptor();

  /**
   * @return the plugin's location. Non null.
   */
  URL getLocation();

  /**
   * Given a {@code pluginLocation} it will generate a {@link Plugin} with the relevant information about it.
   *
   * @param pluginLocation the folder where the plugin relies in. Non null.
   * @return a representation of the plugin in memory.
   * @throws MalformedPluginException if an error occurs when loading the folder, classloadermodel file is missing, etc.
   */
  static Plugin from(File pluginLocation) throws MalformedPluginException {
    try {
      if (pluginLocation.getName().endsWith("zip")) {
        return new DefaultPlugin(new ZipPluginDescriptorLoader().load(pluginLocation), pluginLocation.toURI().toURL());
      } else {
        return new DefaultPlugin(new FolderPluginDescriptorLoader().load(pluginLocation), pluginLocation.toURI().toURL());
      }
    } catch (MalformedURLException e) {
      throw new MalformedPluginException(format("Can not construct URL for %s", pluginLocation.getAbsolutePath()), e);
    }
  }
}
