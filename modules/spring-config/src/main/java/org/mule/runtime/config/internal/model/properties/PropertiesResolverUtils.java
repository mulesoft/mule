/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.properties;

import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_RESERVED_PROPERTIES;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Provides a common set of utilities for handling property resolvers for Mule artifacts.
 *
 */
public class PropertiesResolverUtils {

  private static final AtomicBoolean configured = new AtomicBoolean();

  private PropertiesResolverUtils() {
    // Nothing to do
  }

  /**
   * Configures {@link FeatureFlaggingService} to revert MULE-17659 for applications with <code>minMuleVersion</code> lesser than
   * or equal to 4.2.2, or if system property {@link MuleRuntimeFeature#HONOUR_RESERVED_PROPERTIES} is set. See MULE-17659 and
   * MULE-19038.
   *
   * @since 4.4.0 4.3.0
   */
  public static void configurePropertiesResolverFeatureFlag() {

    if (!configured.getAndSet(true)) {
      FeatureFlaggingRegistry ffRegistry = FeatureFlaggingRegistry.getInstance();

      ffRegistry.registerFeature(HONOUR_RESERVED_PROPERTIES,
                                 ctx -> ctx.getConfiguration().getMinMuleVersion().isPresent()
                                     && ctx.getConfiguration().getMinMuleVersion().get().newerThan("4.2.2"));
    }
  }

}
