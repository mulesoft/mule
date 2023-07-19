/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.context;

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
