/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManagerAdapterFactory;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapterFactory;

import java.net.URL;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConfigurationBuilder} that registers a {@link ExtensionManager}
 *
 * @since 4.0
 */
public class ApplicationExtensionsManagerConfigurationBuilder extends AbstractConfigurationBuilder {

  private static Logger LOGGER = LoggerFactory.getLogger(ApplicationExtensionsManagerConfigurationBuilder.class);

  private final ExtensionManagerAdapterFactory extensionManagerAdapterFactory;
  private final List<ArtifactPlugin> artifactPlugins;

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins) {
    this(artifactPlugins, new DefaultExtensionManagerAdapterFactory());
  }

  public ApplicationExtensionsManagerConfigurationBuilder(List<ArtifactPlugin> artifactPlugins,
                                                          ExtensionManagerAdapterFactory extensionManagerAdapterFactory) {
    this.artifactPlugins = artifactPlugins;
    this.extensionManagerAdapterFactory = extensionManagerAdapterFactory;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    final ExtensionManagerAdapter extensionManager = createExtensionManager(muleContext);

    for (ArtifactPlugin artifactPlugin : artifactPlugins) {
      URL manifestUrl =
          artifactPlugin.getArtifactClassLoader().findResource("META-INF/" + EXTENSION_MANIFEST_FILE_NAME);
      if (manifestUrl == null) {
        continue;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Discovered extension " + artifactPlugin.getArtifactName());
      }
      ExtensionManifest extensionManifest = extensionManager.parseExtensionManifestXml(manifestUrl);
      extensionManager.registerExtension(extensionManifest, artifactPlugin.getArtifactClassLoader().getClassLoader());
    }
  }

  private ExtensionManagerAdapter createExtensionManager(MuleContext muleContext) throws InitialisationException {
    try {
      return extensionManagerAdapterFactory.createExtensionManager(muleContext);
    } catch (Exception e) {
      throw new InitialisationException(e, muleContext);
    }
  }
}
