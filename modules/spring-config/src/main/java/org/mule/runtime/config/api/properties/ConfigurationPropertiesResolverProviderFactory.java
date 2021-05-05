/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import static org.mule.runtime.config.internal.model.properties.PropertiesResolverUtils.createConfigurationAttributeResolver;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;

import java.util.Map;
import java.util.Optional;

/**
 * Provides a factory method for creating a new {@link ConfigurationPropertiesResolverProvider}.
 *
 * @since 4.4
 */
public final class ConfigurationPropertiesResolverProviderFactory {

  private ConfigurationPropertiesResolverProviderFactory() {
    // Nothing to do
  }

  /**
   * @param artifactAst                   the artifact to scan for additional resolvers.
   * @param parentConfigurationProperties the properties from the parent artifact.
   * @param deploymentProperties          the deployment properties of the artifact.
   * @param artifactClassLoader           the classloader of the artifact.
   * @param featureFlaggingService        the featureFlagggingService for the artifact
   * @return a fresh ConfigurationPropertiesResolverProvider for the artifact according to the provided parameters.
   */
  public static ConfigurationPropertiesResolverProvider createConfigurationPropertiesResolverProvider(ArtifactAst artifactAst,
                                                                                                      Optional<ConfigurationProperties> parentConfigurationProperties,
                                                                                                      Map<String, String> deploymentProperties,
                                                                                                      ClassLoader artifactClassLoader,
                                                                                                      Optional<FeatureFlaggingService> featureFlaggingService) {
    return createConfigurationAttributeResolver(artifactAst, parentConfigurationProperties, deploymentProperties,
                                                new ClassLoaderResourceProvider(artifactClassLoader), featureFlaggingService);
  }

}
