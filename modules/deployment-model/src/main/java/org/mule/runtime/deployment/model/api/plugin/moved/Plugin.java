/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.moved;

import static java.lang.String.format;
import org.mule.runtime.deployment.model.internal.plugin.moved.DefaultPlugin;
import org.mule.runtime.deployment.model.internal.plugin.moved.loader.FolderPluginDescriptorLoader;
import org.mule.runtime.deployment.model.internal.plugin.moved.loader.ZipPluginDescriptorLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Marker interface to load {@link PluginDescriptor} from several sources (ZIP, a folder)
 *
 * @since 4.0
 */
public interface Plugin {

  /**
   * @return the descriptor of the current plugin. Non null.
   */
  PluginDescriptor getPluginDescriptor();

  /**
   * @return the plugin's location. Non null.
   */
  URL getLocation();

  /**
   * TODO MULE-10785 change this method to receive an URL rather than a File, and pick the best PluginDescriptor loader by it's extension
   *
   * Given a {@code pluginFolder} it will generate a {@link PluginDescriptor} with the relevant information about it
   *
   * @param pluginFolder the folder where the plugin relies in. Non null.
   * @return a representation of the plugin in memory.
   * @throws MalformedPluginException if an error occurs when loading the folder, descriptor file is missing, etc.
   */
  static Plugin fromFolder(File pluginFolder) throws MalformedPluginException {
    try {
      return new DefaultPlugin(new FolderPluginDescriptorLoader().load(pluginFolder), pluginFolder.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new MalformedPluginException(format("Can not construct URL for %s", pluginFolder.getAbsolutePath()), e);
    }
  }

  /**
   * TODO MULE-10785 change this method to receive an URL rather than a File, and pick the best PluginDescriptor loader by it's extension
   *
   * Given a {@code pluginZip} it will generate a {@link PluginDescriptor} with the relevant information about it
   *
   * @param pluginZip the zip file where the plugin is compressed. Non null.
   * @return a representation of the plugin in memory.
   * @throws MalformedPluginException if an error occurs when loading the folder, descriptor file is missing, etc.
   */
  static Plugin fromZip(File pluginZip) throws MalformedPluginException {
    try {
      return new DefaultPlugin(new ZipPluginDescriptorLoader().load(pluginZip), pluginZip.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new MalformedPluginException(format("Can not construct URL for %s", pluginZip.getAbsolutePath()), e);
    }
  }
}
