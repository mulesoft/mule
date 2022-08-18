/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.internal.classloader;

import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_EXPLICIT_GC_WHEN_DISPOSING_ARTIFACT;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This version of {@link RegionClassLoader} has a reference to the {@link FeatureFlaggingService} and is capable of modify its
 * behavior based on feature flagging.
 */
public class FeatureFlaggingAwareRegionClassLoader extends RegionClassLoader {

  private static final AtomicBoolean regionClassloaderFeaturesSet = new AtomicBoolean(false);

  /**
   * Configures the feature flags related to {@link RegionClassLoader}.
   */
  public static void configureRegionClassLoaderFeatureFlags() {
    // TODO: Refactor Feature Flagging Service Initialization
    if (!regionClassloaderFeaturesSet.getAndSet(true)) {
      FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
      featureFlaggingRegistry.registerFeatureFlag(DISABLE_EXPLICIT_GC_WHEN_DISPOSING_ARTIFACT,
                                                  featureContext -> featureContext.getArtifactMinMuleVersion()
                                                      .filter(muleVersion -> muleVersion
                                                          .atLeast(DISABLE_EXPLICIT_GC_WHEN_DISPOSING_ARTIFACT
                                                              .getEnabledByDefaultSince()))
                                                      .isPresent());
    }
  }

  /**
   * Creates a new region.
   *
   * @param artifactId             artifact unique ID for the artifact owning the created class loader instance. Not empty.
   * @param artifactDescriptor     descriptor for the artifact owning the created class loader instance. Not null.
   * @param parent                 parent classloader for the region. Not null.
   * @param lookupPolicy           lookup policy to use on the region.
   * @param featureFlaggingService the feature flagging service. Not null.
   */
  public FeatureFlaggingAwareRegionClassLoader(String artifactId,
                                               ArtifactDescriptor artifactDescriptor,
                                               ClassLoader parent,
                                               ClassLoaderLookupPolicy lookupPolicy,
                                               FeatureFlaggingService featureFlaggingService) {
    super(artifactId, artifactDescriptor, parent, lookupPolicy, () -> {
      // Checks if we need to skip the explicit GC call
      if (featureFlaggingService.isEnabled(DISABLE_EXPLICIT_GC_WHEN_DISPOSING_ARTIFACT)) {
        return;
      }

      EXPLICIT_GC_RELEASER.release();
    });
  }
}
