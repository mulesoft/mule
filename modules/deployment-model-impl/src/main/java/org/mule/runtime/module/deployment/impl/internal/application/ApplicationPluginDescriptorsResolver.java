/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;

import java.util.List;
import java.util.Set;

/**
 * Resolves the list of artifact plugins descriptors for an application based on the domain plugins descriptors.
 *
 * @since 4.1
 */
public interface ApplicationPluginDescriptorsResolver {

  /**
   * It will check if a plugin declared by the application is already present in the domain's and check if the plugin versions
   * are compatible, if not it will throw an exception.
   * <p/>
   * For those plugins declared by the application but not present in the list of resolved plugins for the domain will be added
   * to the returned list of artifact plugin descriptors.
   *
   * @param domainPluginDescriptors plugins descriptors already resolved for the Domain artifact.
   * @param applicationArtifactPluginDescriptorsDeclared plugins descriptors declared by the application artifact.
   * @return {@link List} of {@link ArtifactPluginDescriptor} that were resolved for the application, filtering the ones already present in
   * domain.
   */
  List<ArtifactPluginDescriptor> resolveArtifactPluginDescriptors(Set<ArtifactPluginDescriptor> domainPluginDescriptors,
                                                                  Set<ArtifactPluginDescriptor> applicationArtifactPluginDescriptorsDeclared);

}
