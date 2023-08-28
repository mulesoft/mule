/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;

/**
 * Constants to be consumed across modules to generate and consume a proper {@link AbstractMuleArtifactModel} when working with
 * the {@link MulePluginModel#getExtensionModelLoaderDescriptor()}.
 *
 * @since 4.0
 * @deprecated since 4.5.0 use {@link org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants} instead
 */
@Deprecated
public class ArtifactDescriptorConstants extends org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants {

  private ArtifactDescriptorConstants() {

  }
}
