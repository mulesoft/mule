/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;

/**
 * Constants to be consumed across modules to generate and consume a proper {@link org.mule.runtime.api.deployment.meta.AbstractMuleArtifactModel} when working with
 * the {@link MulePluginModel#getExtensionModelLoaderDescriptor()}.
 *
 * @since 4.0
 */
public final class ArtifactDescriptorConstants {

  /**
   * Default descriptor loader ID for Mule artifacts
   */
  public static final String MULE_LOADER_ID = "mule";

  /**
   * Property to fill the {@link MuleArtifactLoaderDescriptor#getAttributes()} which defines the exported packages of a given artifact.
   */
  public static final String EXPORTED_PACKAGES = "exportedPackages";

  public static final String PRIVILEGED_EXPORTED_PACKAGES = "privilegedExportedPackages";

  public static final String PRIVILEGED_ARTIFACTS_IDS = "privilegedArtifactIds";

  /**
   * Property to fill the {@link MuleArtifactLoaderDescriptor#getAttributes()} which defines the exported resources of a given artifact.
   */
  public static final String EXPORTED_RESOURCES = "exportedResources";

  /**
   * Property that defines to include or not scope test dependencies when building class loader model of a given artifact.
   */
  public static final String INCLUDE_TEST_DEPENDENCIES = "includeTestDependencies";

}
