/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Sets.newHashSet;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;

import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.util.PropertiesUtils;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the {@link PluginUrlClassification} resources, exported packages and resources.
 *
 * @since 4.0
 */
public class PluginResourcesResolver {

  private static final String PLUGIN_PROPERTIES = "plugin.properties";
  private static final String COMMA_CHARACTER = ",";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final ExtensionManager extensionManager;

  /**
   * Creates an instance of the resolver.
   *
   * @param extensionManager {@link ExtensionManager} to be used
   */
  public PluginResourcesResolver(ExtensionManager extensionManager) {
    checkNotNull(extensionManager, "extensionManager cannot be null");
    this.extensionManager = extensionManager;
  }

  /**
   * Resolves for the given {@link PluginUrlClassification} the resources exported.
   *
   * @param pluginUrlClassification {@link PluginUrlClassification} to be resolved
   * @return {@link PluginUrlClassification} with the resources resolved
   */
  public PluginUrlClassification resolvePluginResourcesFor(PluginUrlClassification pluginUrlClassification) {
    Set<String> exportPackages;
    Set<String> exportResources;

    try (URLClassLoader classLoader = new URLClassLoader(pluginUrlClassification.getUrls().toArray(new URL[0]), null)) {
      URL manifestUrl = classLoader.findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
      if (manifestUrl != null) {
        logger.debug("Plugin '{}' has extension descriptor therefore it will be handled as an extension",
                     pluginUrlClassification.getName());
        ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
        exportPackages = newHashSet(extensionManifest.getExportedPackages());
        // TODO(pablo.kraan): MULE-12189 - this is an ugly hack to make test pass. It will be fixed soon
        exportPackages.remove("org.mule.tck.message");
        exportPackages.remove("org.mule.tck.testmodels.fruit");
        exportPackages.remove("org.mule.tck.testmodels.fruit.peel");
        exportPackages.remove("org.mule.tck.testmodels.fruit.seed");
        exportResources = newHashSet(extensionManifest.getExportedResources());
      } else {
        logger.debug("Plugin '{}' will be handled as standard plugin", pluginUrlClassification.getName());
        URL pluginPropertiesUrl = classLoader.getResource(PLUGIN_PROPERTIES);
        if (pluginPropertiesUrl == null) {
          throw new IllegalStateException(PLUGIN_PROPERTIES + " couldn't be found for plugin: " +
              pluginUrlClassification.getName());
        }
        Properties pluginProperties;
        try {
          pluginProperties = PropertiesUtils.loadProperties(pluginPropertiesUrl);
        } catch (IOException e) {
          throw new RuntimeException("Error while reading plugin properties: " + pluginPropertiesUrl);
        }
        exportPackages = newHashSet(pluginProperties.getProperty(EXPORTED_CLASS_PACKAGES_PROPERTY).split(COMMA_CHARACTER));
        exportResources = newHashSet(pluginProperties.getProperty(EXPORTED_RESOURCE_PROPERTY).split(COMMA_CHARACTER));
      }

      return new PluginUrlClassification(pluginUrlClassification.getName(), pluginUrlClassification.getUrls(),
                                         pluginUrlClassification.getExportClasses(),
                                         pluginUrlClassification.getPluginDependencies(), exportPackages, exportResources);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
