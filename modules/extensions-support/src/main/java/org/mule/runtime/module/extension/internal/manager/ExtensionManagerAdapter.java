/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.manifest.ExtensionManifest;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.net.URL;
import java.util.Optional;

/**
 * Extends the {@link ExtensionManager} interface with contracts which are not to be exposed on the public API
 *
 * @since 4.0
 */
public interface ExtensionManagerAdapter extends ExtensionManager {

  /**
   * Locates and returns the {@link ConfigurationProvider} which would serve an invocation to the
   * {@link #getConfiguration(ExtensionModel, MuleEvent)} method.
   * <p>
   * This means that the returned provider will be located using the same set of rules as the aforementioned method
   *
   * @param extensionModel the {@link ExtensionModel} for which a configuration is wanted
   * @return an {@link Optional} {@link ConfigurationProvider}
   */
  Optional<ConfigurationProvider> getConfigurationProvider(ExtensionModel extensionModel);

  /**
   * Locates and returns the {@link ConfigurationProvider} which would serve an invocation to the
   * {@link #getConfiguration(String, MuleEvent)} method.
   * <p>
   * This means that the returned provided will be located using the same set of rules as the aforementioned method.
   *
   * @param configurationProviderName the name of a previously registered {@link ConfigurationProvider}
   * @return an {@link Optional} {@link ConfigurationProvider}
   */
  Optional<ConfigurationProvider> getConfigurationProvider(String configurationProviderName);

  /**
   * Registered the given {@link ConfigurationProvider} which should be later be used to serve invocations to
   * {@link #getConfigurationProvider(ExtensionModel)} and {@link #getConfiguration(String, MuleEvent)}
   *
   * @param configurationProvider a {@link ConfigurationProvider}
   */
  void registerConfigurationProvider(ConfigurationProvider configurationProvider);

  /**
   * Deserializes an {@link ExtensionManifest} in {@code XML} format
   *
   * @param manifestUrl the {@link URL} to a file which contains the input data
   * @return a {@link ExtensionManifest}
   */
  ExtensionManifest parseExtensionManifestXml(URL manifestUrl);
}
