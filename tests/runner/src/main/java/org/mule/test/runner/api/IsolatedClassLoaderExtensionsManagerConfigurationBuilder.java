/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_AUTO_GENERATED_ARTIFACT_PATH_INSIDE_JAR;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.api.manager.DefaultExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.mule.runtime.core.api.config.ConfigurationBuilder} that creates an
 * {@link org.mule.runtime.core.api.extension.ExtensionManager}. It reads the extension manifest file using the extension class
 * loader that loads the extension annotated class and register the extension to the manager.
 *
 * @since 4.0
 */
public class IsolatedClassLoaderExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  private static final String META_INF_MULE_ARTIFACT_MULE_PLUGIN = "META-INF/mule-artifact/mule-artifact.json";
  private static Logger LOGGER = LoggerFactory.getLogger(IsolatedClassLoaderExtensionsManagerConfigurationBuilder.class);

  private final ExtensionManagerFactory extensionManagerFactory;
  private final List<ArtifactClassLoader> pluginsClassLoaders;
  private final List<ExtensionModel> extensionModels = new ArrayList<>();

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
    this.extensionManagerFactory = new DefaultExtensionManagerFactory();
    this.pluginsClassLoaders = pluginsClassLoaders;
  }

  /**
   * Goes through the list of plugins {@link ArtifactClassLoader}s to check if they have an extension descriptor and if they do it
   * will parse it and register the extension into the {@link org.mule.runtime.core.api.extension.ExtensionManager}
   * <p/>
   * It has to use reflection to access these classes due to the current execution of this method would be with the application
   * {@link ArtifactClassLoader} and the list of plugin {@link ArtifactClassLoader} was instantiated with the Launcher
   * {@link ClassLoader} so casting won't work here.
   *
   * @param muleContext The current {@link org.mule.runtime.core.api.MuleContext}
   * @throws Exception if an error occurs while registering an extension of calling methods using reflection.
   */
  @Override
  protected void doConfigure(final MuleContext muleContext) throws Exception {
    final ExtensionManager extensionManager = createExtensionManager(muleContext);

    for (ExtensionModel extensionModel : extensionModels) {
      extensionManager.registerExtension(extensionModel);
    }
  }

  /**
   * Creates an {@link ExtensionManager} to be used for registering the extensions.
   *
   * @param muleContext a {@link MuleContext} needed for creating the manager
   * @return an {@link ExtensionManager}
   * @throws InitialisationException if an error occurs while initializing the manager.
   */
  private ExtensionManager createExtensionManager(final MuleContext muleContext) throws InitialisationException {
    if (muleContext.getExtensionManager() != null) {
      return muleContext.getExtensionManager();
    }
    ExtensionManager extensionManager = extensionManagerFactory.create(muleContext);

    muleContext.setExtensionManager(extensionManager);

    return extensionManager;
  }

  public void loadExtensionModels() {
    try {
      loadRuntimeExtensionModels().forEach(extensionModels::add);

      for (Object pluginClassLoader : pluginsClassLoaders) {
        String artifactName = (String) pluginClassLoader.getClass().getMethod("getArtifactId").invoke(pluginClassLoader);
        ClassLoader classLoader =
            (ClassLoader) pluginClassLoader.getClass().getMethod("getClassLoader").invoke(pluginClassLoader);
        withContextClassLoader(classLoader, (CheckedRunnable) () -> {
          Method findResource = classLoader.getClass().getMethod("findResource", String.class);
          URL json = ((URL) findResource.invoke(classLoader, META_INF_MULE_ARTIFACT_MULE_PLUGIN));
          if (json == null) {
            json = ((URL) findResource.invoke(classLoader, MULE_AUTO_GENERATED_ARTIFACT_PATH_INSIDE_JAR));
          }
          if (json != null) {
            LOGGER.debug("Discovered extension '{}'", artifactName);
            MulePluginBasedLoaderFinder finder = new MulePluginBasedLoaderFinder(json.openStream());
            if (finder.isExtensionModelLoaderDescriptorDefined()) {
              ExtensionModel extension =
                  finder.getLoader().loadExtensionModel(classLoader, getDefault(emptySet()), finder.getParams());
              extensionModels.add(extension);
            } else {
              LOGGER
                  .debug("Discarding plugin with artifactName '{}' as it doesn't have an ExtensionModelLoaderDescriptor defined",
                         artifactName);
            }
          } else {
            LOGGER.debug("Discarding plugin with artifactName '{}' as it doesn't have a mule-artifact.json", artifactName);
          }
        });

      }
    } catch (Exception e) {
      throw new RuntimeException("Error while loading extension models", e);
    }
  }

  private List<ExtensionModel> loadRuntimeExtensionModels() {
    return new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, Thread.currentThread().getContextClassLoader())
        .stream().map(RuntimeExtensionModelProvider::createExtensionModel).collect(toList());
  }
}
