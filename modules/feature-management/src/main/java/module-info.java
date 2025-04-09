/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * This module performs feature management for the runtime.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.featureManagement {

  requires org.mule.runtime.api;
  requires org.mule.runtime.profiling.api;

  requires com.github.benmanes.caffeine;
  requires togglz.core;

  exports org.mule.runtime.feature.api.management;

  exports org.mule.runtime.feature.internal.config to
      org.mule.runtime.core,
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.feature.internal.config.profiling to
      org.mule.runtime.core;
  exports org.mule.runtime.feature.internal.togglz.config to
      org.mule.runtime.core;
  exports org.mule.runtime.feature.internal.togglz.user to
      org.mule.runtime.core;

  provides org.togglz.core.spi.FeatureManagerProvider
      with org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider;

}
