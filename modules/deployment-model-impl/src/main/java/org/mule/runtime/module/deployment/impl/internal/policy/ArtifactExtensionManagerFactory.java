/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.lang.String.format;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactExtensionManagerConfigurationBuilder.META_INF_FOLDER;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader.VERSION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.internal.loader.java.JavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Creates {@link ExtensionManager} for mule artifacts that own a {@link MuleContext}
 */
public class ArtifactExtensionManagerFactory implements ExtensionManagerFactory {

  private static Logger LOGGER = getLogger(ArtifactExtensionManagerFactory.class);

  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final List<ArtifactPlugin> artifactPlugins;
  private final ExtensionManagerFactory extensionManagerFactory;

  /**
   * Creates a extensionManager factory
   *
   * @param artifactPlugins artifact plugins deployed inside the artifact. Non null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param extensionManagerFactory creates the {@link ExtensionManager} for the artifact. Non null
   */
  public ArtifactExtensionManagerFactory(List<ArtifactPlugin> artifactPlugins,
                                         ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                         ExtensionManagerFactory extensionManagerFactory) {
    this.artifactPlugins = artifactPlugins;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.extensionManagerFactory = extensionManagerFactory;
  }

  @Override
  public ExtensionManager create(MuleContext muleContext) {
    final ExtensionManager extensionManager = createExtensionManager(muleContext);

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

    return extensionManager;
  }

  /**
   * Looks for an extension using the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} file, where if available it will parse it
   * using the {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() ID} matches the plugin's
   * descriptor ID.
   *
   * @param artifactPlugin   to introspect for the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} and further resources from its {@link ClassLoader}
   * @param extensionManager object to store the generated {@link ExtensionModel} if exists
   * @throws IllegalArgumentException if the {@link MulePluginModel#getExtensionModelLoaderDescriptor()} is present, and
   *                                  the ID in it wasn't discovered through SPI.
   */
  private void discoverExtensionThroughJsonDescriber(ArtifactPlugin artifactPlugin, ExtensionManager extensionManager) {
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

  private ExtensionManager createExtensionManager(MuleContext muleContext) {
    ExtensionManager extensionManager = extensionManagerFactory.create(muleContext);

    ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);

    return extensionManager;
  }
}
