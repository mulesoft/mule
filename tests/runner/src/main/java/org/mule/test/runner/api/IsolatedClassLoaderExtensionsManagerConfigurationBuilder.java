/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.mule.runtime.core.api.config.ConfigurationBuilder} that creates an
 * {@link org.mule.runtime.extension.api.ExtensionManager}. It reads the extension manifest file using the extension class loader
 * that loads the extension annotated class and register the extension to the manager.
 *
 * @since 4.0
 */
public class IsolatedClassLoaderExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  private static Logger LOGGER = LoggerFactory.getLogger(IsolatedClassLoaderExtensionsManagerConfigurationBuilder.class);

  private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
  private final List<ArtifactClassLoader> pluginsClassLoaders;

  /**
   * Creates an instance of the builder with the list of plugin class loaders. If an {@link ArtifactClassLoader} has a extension
   * descriptor it will be registered as an extension if not it is assumed that it is not an extension plugin. The extension will
   * be loaded and registered with its corresponding class loader in order to get access to the isolated {@link ClassLoader}
   * defined for the extension.
   *
   * @param pluginsClassLoaders the list of {@link ArtifactClassLoader} created for each plugin found in the dependencies (either
   *        plugin or extension plugin).
   */
  public IsolatedClassLoaderExtensionsManagerConfigurationBuilder(final List<ArtifactClassLoader> pluginsClassLoaders) {
    this.extensionManagerAdapterFactory = new DefaultExtensionManagerAdapterFactory();
    this.pluginsClassLoaders = pluginsClassLoaders;
  }

  /**
   * Goes through the list of plugins {@link ArtifactClassLoader}s to check if they have an extension descriptor and if they do it
   * will parse it and register the extension into the {@link org.mule.runtime.extension.api.ExtensionManager}
   * <p/>
   * It has to use reflection to access these classes due to the current execution of this method would be with the applciation
   * {@link ArtifactClassLoader} and the list of plugin {@link ArtifactClassLoader} was instantiated with the Launcher
   * {@link ClassLoader} so casting won't work here.
   *
   * @param muleContext The current {@link org.mule.runtime.core.api.MuleContext}
   * @throws Exception if an error occurs while registering an extension of calling methods using reflection.
   */
  @Override
  protected void doConfigure(final MuleContext muleContext) throws Exception {
    final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

    for (Object pluginClassLoader : pluginsClassLoaders) {
      String artifactName = (String) pluginClassLoader.getClass().getMethod("getArtifactId").invoke(pluginClassLoader);
      ClassLoader classLoader = (ClassLoader) pluginClassLoader.getClass().getMethod("getClassLoader").invoke(pluginClassLoader);
      URL manifestUrl = getExtensionManifest(classLoader);
      if (manifestUrl != null) {
        LOGGER.debug("Discovered extension: {}", artifactName);
        ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
        extensionManager.registerExtension(extensionManifest, classLoader);
      } else {
        LOGGER.debug(
                     "Discarding plugin artifact class loader with artifactName '{}' due to it doesn't have an extension descriptor",
                     artifactName);
      }
    }
  }

  /**
   * Gets the extension manifest as {@link URL}
   *
   * @param classLoader the plugin {@link ClassLoader} to look for the resource
   * @return a {@link URL} or null if it is not present
   * @throws NoSuchMethodException if findResources {@link Method} is no found by reflection
   * @throws IllegalAccessException if findResources {@link Method} cannot be accessed
   * @throws InvocationTargetException if findResources {@link Method} throws an error
   */
  private URL getExtensionManifest(final ClassLoader classLoader)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    Method findResourceMethod = classLoader.getClass().getMethod("findResource", String.class);
    findResourceMethod.setAccessible(true);
    return (URL) findResourceMethod.invoke(classLoader, "META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
  }

  /**
   * Creates an {@link ExtensionManagerAdapter} to be used for registering the extensions.
   *
   * @param muleContext a {@link MuleContext} needed for creating the manager
   * @return an {@link ExtensionManagerAdapter}
   * @throws InitialisationException if an error occurrs while initializing the manager.
   */
  private ExtensionManagerAdapter createExtensionManager(final MuleContext muleContext) throws InitialisationException {
    try {
      return extensionManagerAdapterFactory.createExtensionManager(muleContext);
    } catch (Exception e) {
      throw new InitialisationException(e, muleContext);
    }
  }
}
