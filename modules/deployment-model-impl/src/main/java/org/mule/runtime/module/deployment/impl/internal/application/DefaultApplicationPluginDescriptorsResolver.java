/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation for {@link ApplicationPluginDescriptorsResolver}.
 */
public class DefaultApplicationPluginDescriptorsResolver implements ApplicationPluginDescriptorsResolver {

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ArtifactPluginDescriptor> resolveArtifactPluginDescriptors(Set<ArtifactPluginDescriptor> domainPluginDescriptors,
                                                                         Set<ArtifactPluginDescriptor> applicationArtifactPluginDescriptorsDeclared) {
    List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();

    for (ArtifactPluginDescriptor appPluginDescriptor : applicationArtifactPluginDescriptorsDeclared) {
      Optional<ArtifactPluginDescriptor> pluginDescriptor =
          findPlugin(domainPluginDescriptors, appPluginDescriptor.getBundleDescriptor());

      if (!pluginDescriptor.isPresent()) {
        artifactPluginDescriptors.add(appPluginDescriptor);
      } else if (!isCompatibleVersion(pluginDescriptor.get().getBundleDescriptor().getVersion(),
                                      appPluginDescriptor.getBundleDescriptor().getVersion())) {
        throw new IllegalStateException(
                                        format("Incompatible version of plugin '%s' (%s:%s) found. Application requires version '%s' but domain provides version '%s'",
                                               appPluginDescriptor.getName(),
                                               appPluginDescriptor.getBundleDescriptor().getGroupId(),
                                               appPluginDescriptor.getBundleDescriptor().getArtifactId(),
                                               appPluginDescriptor.getBundleDescriptor().getVersion(),
                                               pluginDescriptor.get().getBundleDescriptor().getVersion()));
      }
    }
    return artifactPluginDescriptors;
  }

  private static Optional<ArtifactPluginDescriptor> findPlugin(Set<ArtifactPluginDescriptor> appPlugins,
                                                               BundleDescriptor bundleDescriptor) {
    for (ArtifactPluginDescriptor appPlugin : appPlugins) {
      if (appPlugin.getBundleDescriptor().getArtifactId().equals(bundleDescriptor.getArtifactId())
          && appPlugin.getBundleDescriptor().getGroupId().equals(bundleDescriptor.getGroupId())) {
        return of(appPlugin);
      }
    }

    return empty();
  }

}
