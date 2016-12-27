/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.VERSION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Implementation of {@link ConfigurationBuilder} that registers a {@link ExtensionManager}
 *
 * @since 4.0
 */
public class ApplicationExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  public static final String META_INF_FOLDER = "META-INF";
  private static Logger LOGGER = getLogger(ApplicationExtensionsManagerConfigurationBuilder.class);

  private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;

  /**
   * Create an instance of the configuration builder that uses the {@link DefaultExtensionManagerAdapterFactory}.
   *
   * @param artifactPlugins {@link List} of {@link ArtifactPlugin ArtifactPlugins} to be registered.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   */
  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                          ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    this(artifactPlugins, new DefaultExtensionManagerAdapterFactory(), extensionModelLoaderRepository);
  }

  /**
   * Create an instance of the configuration builder.
   *
   * @param artifactPlugins {@link List} of {@link ArtifactPlugin ArtifactPlugins} to be registered.
   * @param extensionManagerAdapterFactory {@link ExtensionManagerAdapterFactory} in order to create the {@link ExtensionManagerAdapter}.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   */
  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                          ExtensionManagerAdapterFactory extensionManagerAdapterFactory,
                                                          ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    checkNotNull(artifactPlugins, "artifactPlugins cannot be null");
    checkNotNull(extensionManagerAdapterFactory, "extensionManagerAdapterFactory cannot be null");
    checkNotNull(extensionModelLoaderRepository, "extensionModelLoaderRepository cannot be null");

    this.artifactPlugins = artifactPlugins;
    this.extensionManagerAdapterFactory = extensionManagerAdapterFactory;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

    for (ArtifactPlugin artifactPlugin : artifactPlugins) {
      URL manifestUrl =
          artifactPlugin.getArtifactClassLoader().findResource(META_INF_FOLDER + "/" + EXTENSION_MANIFEST_FILE_NAME);
      if (manifestUrl != null) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Discovered extension " + artifactPlugin.getArtifactName());
        }
        //TODO: Remove when MULE-11136
        ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
        Map<String, Object> params = new HashMap<>();
        params.put(TYPE_PROPERTY_NAME, extensionManifest.getDescriberManifest().getProperties().get("type"));
        params.put(VERSION, extensionManifest.getVersion());

        extensionManager.registerExtension(
                                           new JavaExtensionModelLoader()
                                               .loadExtensionModel(artifactPlugin.getArtifactClassLoader().getClassLoader(),
                                                                   params));
      } else {
        discoverExtensionThroughJsonDescriber(artifactPlugin, extensionManager);
      }
    }
  }

  /**
   * Looks for an extension using the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} file, where if available it will parse it
   * using the {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() id} matches the plugin's
   * descriptor id.
   *
   * @param artifactPlugin   to introspect for the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} and further resources from its {@link ClassLoader}
   * @param extensionManager object to store the generated {@link ExtensionModel} if exists
   * @throws IllegalArgumentException if the {@link MulePluginModel#getExtensionModelLoaderDescriptor()} is present, and
   *                                  the ID in it wasn't discovered through SPI.
   */
  private void discoverExtensionThroughJsonDescriber(ArtifactPlugin artifactPlugin, ExtensionManagerAdapter extensionManager) {
    artifactPlugin.getDescriptor().getExtensionModelDescriptorProperty().ifPresent(descriptorProperty -> {
      final ExtensionModelLoader extensionModelLoader = extensionModelLoaderRepository.getExtensionModelLoader(descriptorProperty)
          .orElseThrow(() -> new IllegalArgumentException(format(
                                                                 "The identifier '%s' does not match with the describers available to generate an ExtensionModel (working with the plugin '%s')",
                                                                 descriptorProperty.getId(),
                                                                 artifactPlugin.getDescriptor().getName())));
      final ExtensionModel extensionModel = extensionModelLoader
          .loadExtensionModel(artifactPlugin.getArtifactClassLoader().getClassLoader(), descriptorProperty.getAttributes());
      extensionManager.registerExtension(extensionModel);
    });
  }

  private ExtensionManagerAdapter createExtensionManager(MuleContext muleContext) throws InitialisationException {
    try {
      return extensionManagerAdapterFactory.createExtensionManager(muleContext);
    } catch (Exception e) {
      throw new InitialisationException(e, muleContext);
    }
  }

}
