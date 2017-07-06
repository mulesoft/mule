/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import static com.google.common.collect.ImmutableMap.copyOf;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.ioc.ObjectProviderConfiguration;

import java.util.Map;

/**
 * Immutable implementation of {@link ObjectProviderConfiguration}
 * 
 * @since 4.0
 */
public class ImmutableObjectProviderConfiguration implements ObjectProviderConfiguration {

  private Map<String, Object> artifactObjects;
  private ConfigurationProperties configurationProperties;

  public ImmutableObjectProviderConfiguration(Map<String, Object> artifactObjects,
                                              ConfigurationProperties configurationProperties) {
    this.artifactObjects = copyOf(artifactObjects);
    this.configurationProperties = configurationProperties;
  }

  @Override
  public Map<String, Object> getArtifactObjects() {
    return artifactObjects;
  }

  @Override
  public ConfigurationProperties getConfigurationProperties() {
    return configurationProperties;
  }
}
