/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;

/**
 * Constants to be consumed across modules to generate and consume a proper {@link MulePluginModel} when working with
 * the {@link MulePluginModel#getExtensionModelLoaderDescriptor()}.
 *
 * @since 4.0
 */
public final class MavenClassLoaderConstants {

  /**
   * ID used to populate a {@link MuleArtifactLoaderDescriptor#getId()}
   */
  // TODO(pablo.kraan): loader - move this constant somewhere else
  public static final String MULE_LOADER_ID = "mule";

  /**
   * Property to fill the {@link MuleArtifactLoaderDescriptor#getAttributes()} which defines the exported packages of the
   * current plugin.
   */
  public static final String EXPORTED_PACKAGES = "exportedPackages";

  public static final String PRIVILEGED_EXPORTED_PACKAGES = "privilegedExportedPackages";

  public static final String PRIVILEGED_ARTIFACTS_IDS = "privilegedArtifactIds";

  /**
   * Property to fill the {@link MuleArtifactLoaderDescriptor#getAttributes()} which defines the exported resources of the
   * current plugin.
   */
  public static final String EXPORTED_RESOURCES = "exportedResources";

}
