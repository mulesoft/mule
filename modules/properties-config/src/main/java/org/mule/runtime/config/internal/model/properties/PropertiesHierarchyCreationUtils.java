/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.properties;

import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_RESERVED_PROPERTIES;
import static org.mule.runtime.config.api.properties.PropertiesResolverUtils.createGlobalPropertiesSupplier;
import static org.mule.runtime.config.api.properties.PropertiesResolverUtils.getConfigurationPropertiesProvidersFromComponents;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides a common set of utilities for handling property resolvers for Mule artifacts.
 */
public class PropertiesHierarchyCreationUtils {

  private PropertiesHierarchyCreationUtils() {
    // Nothing to do
  }

  public static PropertiesResolverConfigurationProperties createConfigurationAttributeResolver(ArtifactAst artifactAst,
                                                                                               Optional<ConfigurationProperties> parentConfigurationProperties,
                                                                                               Map<String, String> deploymentProperties,
                                                                                               ResourceProvider externalResourceProvider,
                                                                                               Optional<FeatureFlaggingService> featureFlaggingService) {

    ConfigurationPropertiesHierarchyBuilder partialResolverBuilder = new ConfigurationPropertiesHierarchyBuilder();

    // MULE-17659: it should behave without the fix for applications made for runtime prior 4.2.2
    if (featureFlaggingService.orElse(f -> true).isEnabled(HONOUR_RESERVED_PROPERTIES)) {
      partialResolverBuilder.withDeploymentProperties(deploymentProperties);
    }

    Supplier<Map<String, ConfigurationProperty>> globalPropertiesSupplier = createGlobalPropertiesSupplier(artifactAst);

    ConfigurationPropertiesResolver partialResolver = partialResolverBuilder
        .withSystemProperties()
        .withEnvironmentProperties()
        .withGlobalPropertiesSupplier(globalPropertiesSupplier)
        .build();

    artifactAst.updatePropertiesResolver(partialResolver);

    ConfigurationPropertiesHierarchyBuilder completeBuilder = new ConfigurationPropertiesHierarchyBuilder()
        .withDeploymentProperties(deploymentProperties)
        .withSystemProperties()
        .withEnvironmentProperties()
        .withPropertiesFile(externalResourceProvider)
        .withGlobalPropertiesSupplier(globalPropertiesSupplier);


    // Some configuration properties providers may depend their parameters on other properties, so we use the
    // partial resolution to create these resolvers, and then complete the entire hierarchy
    getConfigurationPropertiesProvidersFromComponents(artifactAst, externalResourceProvider, partialResolver)
        .forEach(completeBuilder::withApplicationProperties);

    parentConfigurationProperties.ifPresent(completeBuilder::withDomainPropertiesResolver);

    return new PropertiesResolverConfigurationProperties(completeBuilder.build());

  }

  public static PropertiesResolverConfigurationProperties createConfigurationAttributeResolver(Optional<ConfigurationProperties> parentConfigurationProperties,
                                                                                               Map<String, String> deploymentProperties,
                                                                                               ResourceProvider externalResourceProvider) {
    ConfigurationPropertiesHierarchyBuilder builder = new ConfigurationPropertiesHierarchyBuilder()
        .withDeploymentProperties(deploymentProperties)
        .withSystemProperties()
        .withEnvironmentProperties()
        .withPropertiesFile(externalResourceProvider);

    parentConfigurationProperties.ifPresent(builder::withDomainPropertiesResolver);

    return new PropertiesResolverConfigurationProperties(builder.build());
  }

}
