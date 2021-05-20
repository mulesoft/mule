/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.config;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Utility class meant to provide a {@link org.mule.runtime.api.config.FeatureFlaggingService} substitute during the earlier
 * stages of the deployment, when such service is not available yet.
 * 
 * @since 4.4.0
 */
public class FeatureFlaggingUtils {

  private FeatureFlaggingUtils() {}

  /**
   * True if a {@link Feature} must be enabled, assuming that the given {@link ArtifactDescriptor} provides relevant
   * {@link FeatureContext} metadata.
   * 
   * @param feature            The {@link Feature}
   * @param artifactDescriptor Relevant {@link ArtifactDescriptor}
   * @return True if the {@link Feature} must be enabled.
   */
  public static boolean isFeatureEnabled(Feature feature, ArtifactDescriptor artifactDescriptor) {
    FeatureContext featureContext = new FeatureContext(artifactDescriptor.getMinMuleVersion(), artifactDescriptor.getName());
    return FeatureFlaggingRegistry.getInstance().getFeatureFlagConfigurations().get(feature).test(featureContext);
  }

}
