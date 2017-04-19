/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static java.lang.String.format;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactExtensionManagerConfigurationBuilder.META_INF_FOLDER;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.VERSION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    final ExtensionManager extensionManager = extensionManagerFactory.create(muleContext);
    final Set<ExtensionModel> extensions = new HashSet<>();
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
        ClassLoader cl = artifactPlugin.getArtifactClassLoader().getClassLoader();
        extensions.add(new DefaultJavaExtensionModelLoader().loadExtensionModel(cl, getDefault(extensions), params));
      } else {
        discoverExtensionThroughJsonDescriber(artifactPlugin, extensions);
      }
    }

    extensions.forEach(extensionManager::registerExtension);

    return extensionManager;
  }

  /**
   * Looks for an extension using the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} file, where if available it will parse it
   * using the {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() ID} matches the plugin's
   * descriptor ID.
   *
   * @param artifactPlugin   to introspect for the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} and further resources from its {@link ClassLoader}.
   * @param extensions with the previously generated {@link ExtensionModel}s that will be used to generate the current {@link ExtensionModel}
   *                   and store it in {@code extensions} once generated.
   * @throws IllegalArgumentException if the {@link MulePluginModel#getExtensionModelLoaderDescriptor()} is present, and
   *                                  the ID in it wasn't discovered through SPI.
   */
  private void discoverExtensionThroughJsonDescriber(ArtifactPlugin artifactPlugin, Set<ExtensionModel> extensions) {
    artifactPlugin.getDescriptor().getExtensionModelDescriptorProperty().ifPresent(descriptorProperty -> {
      final ExtensionModelLoader extensionModelLoader = extensionModelLoaderRepository.getExtensionModelLoader(descriptorProperty)
          .orElseThrow(() -> new IllegalArgumentException(format(
                                                                 "The identifier '%s' does not match with the describers available to generate an ExtensionModel (working with the plugin '%s')",
                                                                 descriptorProperty.getId(),
                                                                 artifactPlugin.getDescriptor().getName())));
      final ExtensionModel extensionModel = extensionModelLoader
          .loadExtensionModel(artifactPlugin.getArtifactClassLoader().getClassLoader(), getDefault(extensions),
                              descriptorProperty.getAttributes());
      extensions.add(extensionModel);
    });
  }
}
