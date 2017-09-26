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

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;

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
  private ArtifactDeclaration artifactDeclaration;
  private Map<String, String> artifactProperties = emptyMap();
  private ArtifactType artifactType;
  private boolean enableLazyInitialization;
  private boolean disableXmlValidations;
  private List<ServiceConfigurator> serviceConfigurators = emptyList();
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
  public ArtifactDeclaration getArtifactDeclaration() {
    return artifactDeclaration;
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
   * Allows to create an {@link ArtifactContext} that will not check for XSD validations.
   *
   * @return {@code true} if the artifact context will be created without XSD validations, false otherwise.
   */
  public boolean isDisableXmlValidations() {
    return disableXmlValidations;
  }

  /**
   * @return list of {@link ServiceConfigurator} that may add additional services to the {@link ArtifactContext}.
   */
  public List<ServiceConfigurator> getServiceConfigurators() {
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
     * @param artifactDeclaration configuration of the artifact.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setArtifactDeclaration(ArtifactDeclaration artifactDeclaration) {
      artifactContextConfiguration.artifactDeclaration = artifactDeclaration;
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
     * When the {@link ArtifactContext} is created lazily then not all the services or configuration components are created. Only
     * those requested by subsequent calls to {@link ArtifactContext} get created. This means components are created on demand
     * based on request calls to each service exposed.
     *
     * @param enableLazyInitialization true if the {@link ArtifactContext} must be created lazily.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setEnableLazyInitialization(boolean enableLazyInitialization) {
      artifactContextConfiguration.enableLazyInitialization = enableLazyInitialization;
      return this;
    }

    /**
     * Allows to create an {@link ArtifactContext} that will not check for XSD validations.
     *
     * @param disableXmlValidations {@code true} if the artifact context must be created without XSD validations, false otherwise.
     */
    public ArtifactContextConfigurationBuilder setDisableXmlValidations(boolean disableXmlValidations) {
      artifactContextConfiguration.disableXmlValidations = disableXmlValidations;
      return this;
    }

    /**
     * @param serviceConfigurators list of {@link ServiceConfigurator} that register or override services in the
     *        {@link MuleContext}.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setServiceConfigurators(List<ServiceConfigurator> serviceConfigurators) {
      artifactContextConfiguration.serviceConfigurators = serviceConfigurators;
      return this;
    }

    /**
     * @param parentContext the parent {@link MuleContext} of the {@link ArtifactContext} to be created.
     * @return {@code this} builder
     */
    public ArtifactContextConfigurationBuilder setParentContext(MuleContext parentContext) {
      artifactContextConfiguration.parentContext = of(parentContext);
      return this;
    }

    /**
     * @return creates a {@link ArtifactContextConfiguration} with te provided configuration.
     */
    public ArtifactContextConfiguration build() {
      return artifactContextConfiguration;
    }
  }
}
