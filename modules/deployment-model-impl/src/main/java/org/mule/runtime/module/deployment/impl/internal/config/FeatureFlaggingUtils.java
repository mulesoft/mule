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

public class FeatureFlaggingUtils {

  private FeatureFlaggingUtils() {}

  public static boolean isFeatureFlagEnabled(Feature feature, ArtifactDescriptor artifactDescriptor) {
    FeatureContext featureContext = new FeatureContext(artifactDescriptor.getMinMuleVersion(), artifactDescriptor.getName());
    return FeatureFlaggingRegistry.getInstance().getDecoupledConfigurations().get(feature).test(featureContext);
  }

}
