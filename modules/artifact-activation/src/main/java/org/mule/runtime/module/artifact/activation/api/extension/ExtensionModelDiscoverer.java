/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.extension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.internal.extension.DefaultExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.internal.extension.RepositoryLookupExtensionModelGenerator;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.util.Set;
import java.util.function.Function;

/**
 * Provides a way to discover {@link ExtensionModel}s from the Mule Runtime or from {@code mule-plugins} in the context of a
 * deployable artifact.
 * 
 * @since 4.5
 */
public interface ExtensionModelDiscoverer {

  /**
   * Creates an {@link ExtensionModelDiscoverer} that will rely on class introspection for generating the extension models for
   * plugins developed with the Java SDK.
   * 
   * @param classLoaderFactory a way to obtain the classloader for a given plugin.
   * @return a newly created {@link ExtensionModelDiscoverer}.
   */
  static ExtensionModelDiscoverer defaultExtensionModelDiscoverer(Function<ArtifactPluginDescriptor, ArtifactClassLoader> classLoaderFactory) {
    return new DefaultExtensionModelDiscoverer(new RepositoryLookupExtensionModelGenerator(classLoaderFactory));
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
