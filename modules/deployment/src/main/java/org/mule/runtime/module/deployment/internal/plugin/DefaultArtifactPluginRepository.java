/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getContainerAppPluginsFolder;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Default implementation of an {@link ArtifactPluginRepository} that holds in memory the list of artifact plugin descriptors
 * bundled with the container.
 *
 * @since 4.0
 */
public class DefaultArtifactPluginRepository implements ArtifactPluginRepository {

  private volatile List<ArtifactPluginDescriptor> containerArtifactPluginDescriptors;

  private final ArtifactPluginDescriptorFactory pluginDescriptorFactory;

  /**
   * @param pluginDescriptorFactory a {@link ArtifactPluginDescriptorFactory} for creating from the container applications plugins
   *        folder the list of {@link ArtifactPluginDescriptor}'s
   */
  public DefaultArtifactPluginRepository(ArtifactPluginDescriptorFactory pluginDescriptorFactory) {
    checkArgument(pluginDescriptorFactory != null, "Application plugin descriptor factory cannot be null");
    this.pluginDescriptorFactory = pluginDescriptorFactory;
  }

  public List<ArtifactPluginDescriptor> getContainerArtifactPluginDescriptors() {
    if (containerArtifactPluginDescriptors == null) {
      synchronized (this) {
        if (containerArtifactPluginDescriptors == null) {
          containerArtifactPluginDescriptors = unmodifiableList(createApplicationPluginDescriptors());
        }
      }
    }
    return containerArtifactPluginDescriptors;
  }

  /**
   * For each plugin in container application plugins folder it creates an {@link ArtifactPluginDescriptor} for it and
   * adds the descriptor the given list.
   *
   * @return a non null {@link List} of {@link ArtifactPluginDescriptor}
   */
  private List<ArtifactPluginDescriptor> createApplicationPluginDescriptors() {
    List<ArtifactPluginDescriptor> pluginDescriptors = new LinkedList<>();
    File[] pluginFiles = getContainerAppPluginsFolder()
        .listFiles(file -> file.isDirectory() || endsWithIgnoreCase(file.getName(), ".zip"));
    if (pluginFiles != null) {
      for (File pluginFile : pluginFiles) {
        final ArtifactPluginDescriptor appPluginDescriptor = pluginDescriptorFactory.create(pluginFile);
        pluginDescriptors.add(appPluginDescriptor);
      }
      return pluginDescriptors;
    } else {
      return EMPTY_LIST;
    }
  }
}
