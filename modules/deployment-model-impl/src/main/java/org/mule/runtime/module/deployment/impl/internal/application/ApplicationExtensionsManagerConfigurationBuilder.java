/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.lang.Thread.currentThread;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.internal.introspection.describer.XmlBasedDescriber;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  private final Map<String, ExtensionModelLoader> extensionsModelLoaders;

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins) {
    this(artifactPlugins, new DefaultExtensionManagerAdapterFactory());
  }

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                          ExtensionManagerAdapterFactory extensionManagerAdapterFactory) {
    this.artifactPlugins = artifactPlugins;
    this.extensionManagerAdapterFactory = extensionManagerAdapterFactory;
    this.extensionsModelLoaders = getExtensionModelLoaders();
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
        ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
        extensionManager.registerExtension(extensionManifest, artifactPlugin.getArtifactClassLoader().getClassLoader());
      } else {
        discoverExtensionThroughJsonDescriber(artifactPlugin, extensionManager);
      }
    }
  }

  /**
   * Looks for an extension using the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} file, where if available it will parse it calling the
   * {@link XmlBasedDescriber} describer.
   *
   * @param artifactPlugin to introspect for the {@link ArtifactPluginDescriptor#MULE_PLUGIN_JSON} and further resources from its {@link ClassLoader}
   * @param extensionManager object to store the generated {@link ExtensionModel} if exists
   * @throws IllegalArgumentException if the {@link MulePluginModel#getExtensionModelLoaderDescriptor()} is present, and
   * the ID in it wasn't discovered through SPI.
   */
  private void discoverExtensionThroughJsonDescriber(ArtifactPlugin artifactPlugin, ExtensionManagerAdapter extensionManager) {
    artifactPlugin.getDescriptor().getExtensionModelDescriptorProperty().ifPresent(descriptorProperty -> {
      if (!extensionsModelLoaders.containsKey(descriptorProperty.getId())) {
        throw new IllegalArgumentException(format("The identifier '%s' does not match with the describers available to generate an ExtensionModel (working with the plugin '%s', existing identifiers to generate the %s are '%s')",
                                                  descriptorProperty.getId(), artifactPlugin.getDescriptor().getName(),
                                                  ExtensionModel.class.getName(),
                                                  join(",", extensionsModelLoaders.keySet())));
      }
      final ExtensionModelLoader extensionModelLoader = extensionsModelLoaders.get(descriptorProperty.getId());
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

  /**
   * Will look through SPI every class that implements the {@code providerClass} and if there are repeated IDs, it will
   * collect them all to throw an exception with the detailed message.
   * <p/>
   * The exception, if thrown, will have the following message:
   * <pre>
   *   There are several loaders that return the same ID when looking up providers for 'org.mule.runtime.module.artifact.ExtensionModelLoader'. Full error list:
   *   ID [some-id] is being returned by the following classes [org.foo.FooLoader, org.bar.BarLoader]
   *   ID [another-id] is being returned by the following classes [org.foo2.FooLoader2, org.bar2.BarLoader2]
   * </pre>
   *
   * @return a {@link Map} with all the loaders.
   * @throws IllegalStateException if there are loaders with repeated IDs.
   */
  private Map<String, ExtensionModelLoader> getExtensionModelLoaders() {
    final Class<ExtensionModelLoader> providerClass = ExtensionModelLoader.class;
    final SpiServiceRegistry spiServiceRegistry = new SpiServiceRegistry();
    final ClassLoader classLoader = currentThread().getContextClassLoader();

    final Collection<ExtensionModelLoader> extensionModelLoaders =
        spiServiceRegistry.lookupProviders(providerClass, classLoader);
    final StringBuilder sb = new StringBuilder();
    extensionModelLoaders.stream().collect(groupingBy(ExtensionModelLoader::getId))
        .entrySet().stream().filter(entry -> entry.getValue().size() > 1)
        .forEach(
                 entry -> {
                   // At this point we are sure there are at least 2 classes that return the same ID, we will append it to the builder
                   final String classes = entry.getValue().stream()
                       .map(extensionModelLoader -> extensionModelLoader.getClass().getName()).collect(Collectors.joining(", "));
                   sb.append(lineSeparator()).append("ID [").append(entry.getKey())
                       .append("] is being returned by the following classes [").append(classes).append("]");
                 });
    if (isNotBlank(sb.toString())) {
      throw new IllegalStateException(format("There are several loaders that return the same identifier when looking up providers for '%s'. Full error list: %s",
                                             providerClass.getName(), sb.toString()));
    }
    return extensionModelLoaders.stream().collect(Collectors.toMap(ExtensionModelLoader::getId, identity()));
  }
}
