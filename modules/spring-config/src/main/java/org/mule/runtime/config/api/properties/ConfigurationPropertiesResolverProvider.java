/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static org.mule.runtime.config.internal.model.properties.PropertiesResolverUtils.createConfigurationAttributeResolver;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;

import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Specialization of {@link ConfigurationProperties} that may provide instances of a {@link UnaryOperator<String>} instance for
 * resolving configuration properties that handle configuration keys consistently.
 * <p>
 * For instance, for an implementation of this whose {@code resolveProperty} method resolves {@code '${someProp}' ->
 * 'SomeValue!'}, the returned {@link UnaryOperator<String>} will resolve {@code 'The value is ${someProp}' -> 'The value is
 * SomeValue!'}
 *
 * @since 4.4
 */
public interface ConfigurationPropertiesResolverProvider extends ConfigurationProperties {

  /**
   * A builder for creating new {@link ConfigurationPropertiesResolverProvider} instances.
   */
  public static final class Builder {

    private ArtifactAst artifactAst;
    private Optional<ConfigurationProperties> parentConfigurationProperties = empty();
    private Map<String, String> deploymentProperties;
    private ClassLoader artifactClassLoader;
    private Optional<FeatureFlaggingService> featureFlaggingService = empty();

    /**
     * @param artifactAst the artifact to scan for additional resolvers.
     * @return the updated builder.
     */
    public Builder from(ArtifactAst artifactAst) {
      this.artifactAst = artifactAst;
      return this;
    }

    /**
     * @param parentConfigurationProperties the properties from the parent artifact.
     * @return the updated builder.
     */
    public Builder withParentProperties(Optional<ConfigurationProperties> parentConfigurationProperties) {
      this.parentConfigurationProperties = parentConfigurationProperties;
      return this;
    }

    /**
     * @param deploymentProperties the deployment properties of the artifact.
     * @return the updated builder.
     */
    public Builder withDeploymentProperties(Map<String, String> deploymentProperties) {
      this.deploymentProperties = deploymentProperties;
      return this;
    }

    /**
     * @param artifactClassLoader the classloader of the artifact.
     * @return the updated builder.
     */
    public Builder loadingResourcesWith(ClassLoader artifactClassLoader) {
      this.artifactClassLoader = artifactClassLoader;
      return this;
    }

    /**
     * @param featureFlaggingService the featureFlagggingService for the artifact
     * @return the updated builder.
     */
    public Builder withFeatureFlags(Optional<FeatureFlaggingService> featureFlaggingService) {
      this.featureFlaggingService = featureFlaggingService;
      return this;
    }

    /**
     * @return a fresh ConfigurationPropertiesResolverProvider for the artifact according to the provided parameters.
     */
    public ConfigurationPropertiesResolverProvider build() {
      return createConfigurationAttributeResolver(requireNonNull(artifactAst),
                                                  requireNonNull(parentConfigurationProperties),
                                                  requireNonNull(deploymentProperties),
                                                  new ClassLoaderResourceProvider(requireNonNull(artifactClassLoader)),
                                                  requireNonNull(featureFlaggingService));
    }
  }

  /**
   * @return a builder for creating new {@link ConfigurationPropertiesResolverProvider} instances.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Implementations must ensure that many calls to this method on the same object return the same value.
   *
   * @return a {@link UnaryOperator<String>} instance for resolving configuration properties.
   */
  UnaryOperator<String> getConfigurationPropertiesResolver();

}
