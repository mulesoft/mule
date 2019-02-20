/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static com.google.common.base.Preconditions.checkNotNull;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.ExtendedClassLoaderModelAttributes;

import java.util.Map;

/**
 * Allows to extends the attributes defined for a {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel}
 * when it is being loaded by {@link org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader} for plugins in
 * order to define in which deployable artifact the plugin is declared.
 *
 * @since 4.1.5
 */
public class PluginExtendedClassLoaderModelAttributes extends ExtendedClassLoaderModelAttributes {

  private ArtifactDescriptor deployableArtifactDescriptor;

  /**
   * Creates an instance of this extended attributes for the given descriptor.
   *
   * @param originalAttributes the original {@link Map} of attributes. No null.
   * @param deployableArtifactDescriptor {@link ArtifactDescriptor} which declares the plugin dependency. Not null.
   */
  public PluginExtendedClassLoaderModelAttributes(Map originalAttributes,
                                                  ArtifactDescriptor deployableArtifactDescriptor) {
    super(originalAttributes);
    checkNotNull(deployableArtifactDescriptor, "deployableArtifactDescriptor cannot be null");
    this.deployableArtifactDescriptor = deployableArtifactDescriptor;
  }

  /**
   * @return the {@link ArtifactDescriptor} which declares the dependency to the plugin.
   */
  public ArtifactDescriptor getDeployableArtifactDescriptor() {
    return deployableArtifactDescriptor;
  }
}
