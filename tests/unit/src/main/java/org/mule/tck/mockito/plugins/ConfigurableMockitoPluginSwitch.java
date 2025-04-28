/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.mockito.plugins;

import java.util.LinkedList;
import java.util.List;

import org.mockito.plugins.PluginSwitch;

/**
 * This {@code PluginSwitch} allows switching off and on arbitrary mockito plugins.
 *
 * To enable it you should place the file <code>mockito-extensions/org.mockito.plugins.PluginSwitch</code> in your classpath and
 * put inside it the full qualified name of this class.
 *
 * When you want to enable any plugin, just call {@link #enablePlugins(List)} and contrarily to disable any previously enabled
 * ones call {@link #disablePlugins(List)}. Be aware that besides enabling this class, you have to declare also the plugins as
 * usual, by placing the proper files in <code>mockito-extensions/</code>
 *
 * @see PluginSwitch
 *
 * @since 4.4.0, 4.3.1
 */
public class ConfigurableMockitoPluginSwitch implements PluginSwitch {

  private static List<String> pluginsEnabled = new LinkedList<>();

  @Override
  public boolean isEnabled(String pluginClassName) {
    return pluginsEnabled.contains(pluginClassName);
  }

  public static void enablePlugins(List<String> plugins) {
    ConfigurableMockitoPluginSwitch.pluginsEnabled.addAll(plugins);
  }

  public static void disablePlugins(List<String> plugins) {
    ConfigurableMockitoPluginSwitch.pluginsEnabled.removeAll(plugins);
  }

}
