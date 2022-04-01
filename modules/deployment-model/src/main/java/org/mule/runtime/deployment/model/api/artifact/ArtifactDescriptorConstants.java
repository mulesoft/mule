/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.api.deployment.meta.MulePluginModel;

/**
 * Constants to be consumed across modules to generate and consume a proper
 * {@link org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModel} when working with the
 * {@link MulePluginModel#getExtensionModelLoaderDescriptor()}.
 *
 * @since 4.0
 */
@Deprecated
public class ArtifactDescriptorConstants extends org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants {

  private ArtifactDescriptorConstants() {

  }
}
