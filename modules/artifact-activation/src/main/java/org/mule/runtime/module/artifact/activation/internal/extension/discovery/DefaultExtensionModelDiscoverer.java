/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.extension.discovery;

import static org.mule.runtime.extension.api.provider.RuntimeExtensionModelProviderLoaderUtils.discoverRuntimeExtensionModels;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionDiscoveryRequest;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelDiscoverer;
import org.mule.runtime.module.artifact.activation.internal.PluginsDependenciesProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link ExtensionModelDiscoverer}.
 *
 * @since 4.5
 */
public class DefaultExtensionModelDiscoverer implements ExtensionModelDiscoverer {

  private final ExtensionModelGenerator extensionModelGenerator;

  public DefaultExtensionModelDiscoverer(ExtensionModelGenerator extensionModelGenerator) {
    this.extensionModelGenerator = extensionModelGenerator;
  }

  @Override
  public Set<ExtensionModel> discoverPluginsExtensionModels(ExtensionDiscoveryRequest discoveryRequest) {
    Set<ExtensionModel> runtimeExtensionModels = discoverRuntimeExtensionModels();
    List<ExtensionModel> pluginDependenciesExtensionModels = PluginsDependenciesProcessor
        .process(discoveryRequest.getArtifactPluginDescriptors(), discoveryRequest.isParallelDiscovery(),
                 (extensions, artifactPlugin) -> {
                   Set<ExtensionModel> dependencies = new HashSet<>();

                   dependencies.addAll(extensions);
                   dependencies.addAll(discoveryRequest.getParentArtifactExtensions());
                   dependencies.addAll(runtimeExtensionModels);

                   ExtensionModel extension =
                       extensionModelGenerator.obtainExtensionModel(discoveryRequest, artifactPlugin, dependencies);
                   if (extension != null) {
                     extensions.add(extension);
                   }
                 });

    return concat(runtimeExtensionModels.stream(), pluginDependenciesExtensionModels.stream()).collect(toSet());
  }
}
