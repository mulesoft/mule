/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactExtensionManagerConfigurationBuilder.META_INF_FOLDER;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.util.Pair;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.persistence.manifest.ExtensionManifestXmlSerializer;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.ArtifactExtensionManagerFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

/**
 * Discover the {@link ExtensionModel} based on the {@link ExtensionModelLoader} type.
 *
 * @since 4.0
 */
public class ExtensionModelDiscoverer {

  private static Logger LOGGER = getLogger(ArtifactExtensionManagerFactory.class);

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param artifactPlugins {@lin Pair} of {@link ArtifactPluginDescriptor} and {@link ArtifactClassLoader} for artifact plugins deployed inside the artifact. Non null.
   * @return {@link Set} of {@link ExtensionModel} discovered from the {@link List} of artifactPlugins.
   */
  public Set<ExtensionModel> discoverExtensionModels(ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                     List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins) {
    final Set<ExtensionModel> extensions = new HashSet<>();
    artifactPlugins.forEach(artifactPlugin -> {
      final ArtifactPluginDescriptor artifactPluginDescriptor = artifactPlugin.getFirst();
      Optional<LoaderDescriber> loaderDescriber = artifactPluginDescriptor.getExtensionModelDescriptorProperty();
      ClassLoader artifactClassloader = artifactPlugin.getSecond().getClassLoader();
      String artifactName = artifactPluginDescriptor.getName();
      if (loaderDescriber.isPresent()) {
        discoverExtensionThroughJsonDescriber(extensionModelLoaderRepository, loaderDescriber.get(), extensions,
                                              artifactClassloader, artifactName);
      } else {
        URL manifest = artifactPlugin.getSecond().findResource(META_INF_FOLDER + "/" + EXTENSION_MANIFEST_FILE_NAME);
        if (manifest != null) {
          //TODO: Remove when MULE-11136
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Discovered extension " + artifactName);
          }
          discoverExtensionThroughManifest(extensions, artifactClassloader, manifest);
        } else {
          LOGGER.warn("Extension [" + artifactName + "] could not be discovered");
        }
      }
    });
    return extensions;
  }

  /**
   * Parses the extension-manifest.xml file, and gets the extension type and version to use the
   * {@link DefaultJavaExtensionModelLoader} to load the extension.
   *
   * @param extensions with the previously generated {@link ExtensionModel}s that will be used to generate the current {@link ExtensionModel}
   *                   and store it in {@code extensions} once generated.
   * @param artifactClassloader the loaded artifact {@link ClassLoader} to find the required resources.
   * @param manifestUrl the location of the extension-manifest.xml file.
   */
  //TODO: Remove when MULE-11136
  private void discoverExtensionThroughManifest(Set<ExtensionModel> extensions,
                                                ClassLoader artifactClassloader, URL manifestUrl) {
    ExtensionManifest extensionManifest = parseExtensionManifestXml(manifestUrl);
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, extensionManifest.getDescriberManifest().getProperties().get("type"));
    params.put(VERSION, extensionManifest.getVersion());
    extensions.add(new DefaultJavaExtensionModelLoader().loadExtensionModel(artifactClassloader, getDefault(extensions), params));
  }

  //TODO: Remove when MULE-11136
  private ExtensionManifest parseExtensionManifestXml(URL manifestUrl) {
    try (InputStream manifestStream = manifestUrl.openStream()) {
      return new ExtensionManifestXmlSerializer().deserialize(IOUtils.toString(manifestStream));
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not read extension manifest on plugin " + manifestUrl.toString()),
                                     e);
    }
  }

  /**
   * Looks for an extension using the mule-artifact.json file, where if available it will parse it
   * using the {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() ID} matches the plugin's
   * descriptor ID.
   *
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param loaderDescriber a descriptor that contains parametrization to construct an {@link ExtensionModel}
   * @param extensions with the previously generated {@link ExtensionModel}s that will be used to generate the current {@link ExtensionModel}
   *                   and store it in {@code extensions} once generated.
   * @param artifactClassloader the loaded artifact {@link ClassLoader} to find the required resources.
   * @param artifactName the name of the artifact being loaded.
   * @throws IllegalArgumentException there is no {@link ExtensionModelLoader} for the ID in the {@link MulePluginModel}.
   */
  private void discoverExtensionThroughJsonDescriber(ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                     LoaderDescriber loaderDescriber, Set<ExtensionModel> extensions,
                                                     ClassLoader artifactClassloader, String artifactName) {
    ExtensionModelLoader loader = extensionModelLoaderRepository.getExtensionModelLoader(loaderDescriber)
        .orElseThrow(() -> new IllegalArgumentException(format("The identifier '%s' does not match with the describers available "
            + "to generate an ExtensionModel (working with the plugin '%s')",
                                                               loaderDescriber.getId(), artifactName)));
    extensions.add(loader.loadExtensionModel(artifactClassloader, getDefault(extensions), loaderDescriber.getAttributes()));
  }


}
