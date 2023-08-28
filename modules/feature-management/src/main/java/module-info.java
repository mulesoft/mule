/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
      org.mule.runtime.core;
  exports org.mule.runtime.feature.internal.config.profiling to
      org.mule.runtime.core;
  exports org.mule.runtime.feature.internal.togglz.config to
      org.mule.runtime.core;
  exports org.mule.runtime.feature.internal.togglz.user to
      org.mule.runtime.core;
  
  provides org.togglz.core.spi.FeatureManagerProvider
      with org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider;

}
