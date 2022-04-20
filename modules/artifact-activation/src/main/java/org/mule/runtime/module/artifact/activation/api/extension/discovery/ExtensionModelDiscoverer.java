/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension.discovery;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderSupplier;
import org.mule.runtime.module.artifact.activation.internal.extension.discovery.DefaultExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.internal.extension.discovery.RepositoryLookupExtensionModelGenerator;

import java.util.Set;

/**
 * Provides a way to discover {@link ExtensionModel}s from the Mule Runtime or from {@code mule-plugins} in the context of a
 * deployable artifact.
 * 
 * @since 4.5
 */
public interface ExtensionModelDiscoverer {

  /**
   * Creates an {@link ExtensionModelDiscoverer} that will generate the extension models for plugins in a class loader.
   * 
   * @param classLoaderFactory             a way to obtain the classloader for a given plugin.
   * @param extensionModelLoaderRepository repository to manage access to an
   *                                       {@link org.mule.runtime.extension.api.loader.ExtensionModelLoader}.
   * @return a newly created {@link ExtensionModelDiscoverer}.
   */
  static ExtensionModelDiscoverer defaultExtensionModelDiscoverer(PluginClassLoaderSupplier classLoaderFactory,
                                                                  ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    return new DefaultExtensionModelDiscoverer(new RepositoryLookupExtensionModelGenerator(classLoaderFactory,
                                                                                           extensionModelLoaderRepository));
  }

  /**
   * Discovers the extension models provided by the Mule Runtime.
   *
   * @return {@link Set} of the runtime provided {@link ExtensionModel}s.
   */
  Set<ExtensionModel> discoverRuntimeExtensionModels();

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param discoveryRequest an object containing the parameterization of the discovery process.
   * @return The discovered {@link ExtensionModel}s.
   */
  Set<ExtensionModel> discoverPluginsExtensionModels(ExtensionDiscoveryRequest discoveryRequest);

}
