/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.dsl.api.config.ArtifactConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration required for the creation of an {@link ArtifactContext}.
 * 
 * @since 4.0
 */
public class ArtifactContextConfiguration {

  private MuleContext muleContext;
  private String[] configResources;
  private ArtifactConfiguration artifactConfiguration;
  private Map<String, String> artifactProperties = emptyMap();
  private ArtifactType artifactType;
  private boolean enableLazyInitialization;
  private List<MuleContextServiceConfigurator> serviceConfigurators = emptyList();
  private Optional<MuleContext> parentContext = empty();

  private ArtifactContextConfiguration() {}

  /**
   * Creates a new builder for creating instances of {@code ArtifactContextConfiguration}
   * 
   * @return a new builder instance.
   */
  public static ArtifactContextConfigurationBuilder builder() {
    return new ArtifactContextConfigurationBuilder();
  }

  /**
   * @return the {@link MuleContext} of the artifact.
   */
  public MuleContext getMuleContext() {
    return muleContext;
  }

  /**
   * @return configuration files of the artifact.
   */
  public String[] getConfigResources() {
    return configResources;
  }

  /**
   * @return configuration of the artifact.
   */
  public ArtifactConfiguration getArtifactConfiguration() {
    return artifactConfiguration;
  }

  /**
   * @return properties of the artifact. This may be used on runtime or by the artifact configuration.
   */
  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }

  /**
   * @return type of the artifact.
   */
  public ArtifactType getArtifactType() {
    return artifactType;
  }

  /**
   * When an {@link ArtifactContext} is created lazily then not all components will be created during the creation of the
   * artifact. Only the minimum set of required components will be created upon request of functionality.
   * 
   * @return true if the artifact context will be created lazily upon requests to the {@link ArtifactContext}, false otherwise.
   */
  public boolean isEnableLazyInitialization() {
    return enableLazyInitialization;
  }

  /**
   * @return list of {@link MuleContextServiceConfigurator} that may add additional services to the {@link ArtifactContext}.
   */
  public List<MuleContextServiceConfigurator> getServiceConfigurators() {
    return serviceConfigurators;
  }

  /**
   * @return the parent context of this artifact context.
   */
  public Optional<MuleContext> getParentContext() {
    return parentContext;
  }

  /**
   * Builder for {@code ArtifactContextConfiguration}.
   */
  public static class ArtifactContextConfigurationBuilder {

    private ArtifactContextConfiguration artifactContextConfiguration = new ArtifactContextConfiguration();

    /**
     * @param muleContext the artifact {@link MuleContext}
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setMuleContext(MuleContext muleContext) {
      artifactContextConfiguration.muleContext = muleContext;
      return this;
    }

    /**
     * @param configResources configuration files of the artifact.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setConfigResources(String[] configResources) {
      artifactContextConfiguration.configResources = configResources;
      return this;
    }

    /**
     * @param artifactConfiguration configuration of the artifact.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setArtifactConfiguration(ArtifactConfiguration artifactConfiguration) {
      artifactContextConfiguration.artifactConfiguration = artifactConfiguration;
      return this;
    }

    /**
     * @param artifactProperties properties of the artifact. This may be used on runtime or by the artifact configuration.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setArtifactProperties(Map<String, String> artifactProperties) {
      artifactContextConfiguration.artifactProperties = artifactProperties;
      return this;
    }

    /**
     * @param artifactType the type of the artifact. The artifact type restricts the functionality available in the artifact
     *        context.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setArtifactType(ArtifactType artifactType) {
      artifactContextConfiguration.artifactType = artifactType;
      return this;
    }

    /**
     * When the {@link ArtifactContext} is created lazily then
     *
     * @param enableLazyInitialization true if the {@link ArtifactContext} must be created lazily.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setEnableLazyInitialization(boolean enableLazyInitialization) {
      artifactContextConfiguration.enableLazyInitialization = enableLazyInitialization;
      return this;
    }

    /**
     * 
     * @param serviceConfigurators
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setServiceConfigurators(List<MuleContextServiceConfigurator> serviceConfigurators) {
      artifactContextConfiguration.serviceConfigurators = serviceConfigurators;
      return this;
    }

    /**
     * 
     * @param parentContext
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setParentContext(MuleContext parentContext) {
      artifactContextConfiguration.parentContext = of(parentContext);
      return this;
    }

    public ArtifactContextConfiguration build() {
      return artifactContextConfiguration;
    }
  }
}
