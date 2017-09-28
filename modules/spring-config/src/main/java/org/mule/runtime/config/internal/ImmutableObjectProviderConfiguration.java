/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.ioc.ObjectProviderConfiguration;

/**
 * Immutable implementation of {@link ObjectProviderConfiguration}
 * 
 * @since 4.0
 */
public class ImmutableObjectProviderConfiguration implements ObjectProviderConfiguration {

  private final ObjectProvider artifactObjectProvider;
  private final ConfigurationProperties configurationProperties;

  public ImmutableObjectProviderConfiguration(ConfigurationProperties configurationProperties,
                                              ObjectProvider artifactObjectProvider) {
    this.configurationProperties = configurationProperties;
    this.artifactObjectProvider = artifactObjectProvider;
  }

  @Override
  public ObjectProvider getArtifactObjectProvider() {
    return artifactObjectProvider;
  }

  @Override
  public ConfigurationProperties getConfigurationProperties() {
    return configurationProperties;
  }

}
