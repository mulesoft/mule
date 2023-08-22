/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.Optional;
import java.util.Set;

/**
 * Resolves the {@link ArtifactPluginDescriptor} described by the {@link BundleDescriptor}, wrapping the logic to extract it from
 * the jar.
 *
 * @since 4.5
 */
public interface PluginDescriptorResolver {

  /**
   * @return the default implementation of a {@link PluginDescriptorResolver}.
   */
  static PluginDescriptorResolver pluginDescriptorResolver() {
    return (artifactPluginDescriptors, bundleDescriptor) -> artifactPluginDescriptors
        .stream()
        .filter(apd -> apd.getBundleDescriptor().getArtifactId()
            .equals(bundleDescriptor.getArtifactId())
            && apd.getBundleDescriptor().getGroupId()
                .equals(bundleDescriptor.getGroupId()))
        .findAny();
  }

  /**
   * Holds the logic to extract an {@link ArtifactPluginDescriptor} from the jar described by the given {@link BundleDescriptor}.
   * The function must return {@link Optional#empty()} if the plugin represented by the {@link BundleDescriptor} is not a
   * dependency of the artifact for {@code ownerArtifactClassLoader}.
   *
   * @param artifactPluginDescriptors plugin descriptors from which to retrieve the one matching the given
   *                                  {@code bundleDescriptor}.
   * @param bundleDescriptor          the bundle descriptor of the plugin to get the artifact descriptor for.
   * @return optionally returns an {@link ArtifactPluginDescriptor} corresponding to the given {@link BundleDescriptor}.
   */
  Optional<ArtifactPluginDescriptor> resolve(Set<ArtifactPluginDescriptor> artifactPluginDescriptors,
                                             BundleDescriptor bundleDescriptor);

}
