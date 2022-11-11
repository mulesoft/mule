/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.properties;

import static org.mule.runtime.api.component.AbstractComponent.ANNOTATION_NAME;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.api.config.MuleRuntimeFeature.HONOUR_RESERVED_PROPERTIES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import static java.lang.Class.forName;
import static java.lang.String.format;
import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationProperty;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import org.slf4j.Logger;

/**
 * Provides a common set of utilities for handling property resolvers for Mule artifacts.
 */
public class PropertiesResolverUtils {

  private static final Logger LOGGER = getLogger(PropertiesResolverUtils.class);

  public static final String GLOBAL_PROPERTY = "global-property";

  private PropertiesResolverUtils() {
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

  /**
   * Creates the {@link PropertiesResolverConfigurationProperties} with the entire correct hierarchy for an isolated environment.
   * This includes neither System Properties nor Environment Properties.
   * 
   * @param artifactAst              the Artifact AST of the application to calculate the properties resolvers.
   * @param deploymentProperties     a {@link Map} of deployment properties
   * @param externalResourceProvider a {@link ResourceProvider} to use to retrieve files to be used for properties' resolution
   */
  public static PropertiesResolverConfigurationProperties createIsolatedConfigurationAttributeResolver(ArtifactAst artifactAst,
                                                                                                       Map<String, String> deploymentProperties,
                                                                                                       ResourceProvider externalResourceProvider) {
    Supplier<Map<String, ConfigurationProperty>> globalPropertiesSupplier = createGlobalPropertiesSupplier(artifactAst);
    ConfigurationPropertiesResolver partialResolver = new ConfigurationPropertiesHierarchyBuilder()
        .withDeploymentProperties(deploymentProperties)
        .withGlobalPropertiesSupplier(globalPropertiesSupplier)
        .withoutFailuresIfPropertyNotPresent()
        .build();

    artifactAst.updatePropertiesResolver(partialResolver);

    ConfigurationPropertiesHierarchyBuilder completeBuilder = new ConfigurationPropertiesHierarchyBuilder()
        .withDeploymentProperties(deploymentProperties)
        .withPropertiesFile(externalResourceProvider)
        .withGlobalPropertiesSupplier(globalPropertiesSupplier);

    // Some configuration properties providers may depend their parameters on other properties, so we use the
    // partial resolution to create these resolvers, and then complete the entire hierarchy
    getConfigurationPropertiesProvidersFromComponents(artifactAst, externalResourceProvider, partialResolver)
        .forEach(completeBuilder::withApplicationProperties);

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

  public static Supplier<Map<String, ConfigurationProperty>> createGlobalPropertiesSupplier(ArtifactAst artifactAst) {
    return new LazyValue<>(() -> {
      final Map<String, ConfigurationProperty> globalProperties = new HashMap<>();

      artifactAst.topLevelComponentsStream()
          .filter(comp -> GLOBAL_PROPERTY.equals(comp.getIdentifier().getName()))
          .forEach(comp -> {
            final String key = comp.getParameter(DEFAULT_GROUP_NAME, "name").getResolvedRawValue();
            final String rawValue = comp.getParameter(DEFAULT_GROUP_NAME, "value").getRawValue();
            globalProperties.put(key,
                                 new DefaultConfigurationProperty(format("global-property - file: %s - lineNumber %s",
                                                                         comp.getMetadata().getFileName().orElse("(n/a)"),
                                                                         comp.getMetadata().getStartLine().orElse(-1)),
                                                                  key, rawValue));
          });

      return globalProperties;
    });
  }

  private static List<ConfigurationPropertiesProvider> getConfigurationPropertiesProvidersFromComponents(ArtifactAst artifactAst,
                                                                                                         ResourceProvider externalResourceProvider,
                                                                                                         ConfigurationPropertiesResolver localResolver) {

    Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> providerFactoriesMap = loadProviderFactories();

    return artifactAst.topLevelComponentsStream()
        .filter(comp -> providerFactoriesMap.containsKey(comp.getIdentifier()))
        .map(comp -> {
          ConfigurationPropertiesProvider provider = providerFactoriesMap.get(comp.getIdentifier())
              .createProvider(comp, localResolver, externalResourceProvider);
          if (provider instanceof Component) {
            final Map<QName, Object> annotations = new HashMap<>();
            annotations.put(LOCATION_KEY, comp.getLocation());
            annotations.put(ANNOTATION_NAME, comp.getIdentifier());
            annotations.put(SOURCE_ELEMENT_ANNOTATION_KEY,
                            comp.getMetadata().getSourceCode()
                                .map(LocationExecutionContextProvider::maskPasswords)
                                .orElse(null));

            ((Component) provider).setAnnotations(annotations);
          }
          return provider;
        })
        .collect(toList());
  }

  public static Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> loadProviderFactories() {
    Map<ComponentIdentifier, ConfigurationPropertiesProviderFactory> providerFactoriesMap = new HashMap<>();

    ServiceLoader<ConfigurationPropertiesProviderFactory> providerFactories = load(ConfigurationPropertiesProviderFactory.class);
    providerFactories.forEach(service -> {
      ComponentIdentifier componentIdentifier = service.getSupportedComponentIdentifier();
      if (providerFactoriesMap.containsKey(componentIdentifier)) {
        throw new MuleRuntimeException(createStaticMessage("Multiple configuration providers for component: "
            + componentIdentifier));
      }
      providerFactoriesMap.put(componentIdentifier, service);
    });

    // Support of the old deprecated interface only if it is available in the classpath.
    // This may happen only on environments where the runtime modules are uses as libs in some tool, but not when inside the
    // Runtime.
    try {
      Class<?> providerFactoryIfaceOld =
          forName("org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory");

      ServiceLoader<? extends ConfigurationPropertiesProviderFactory> providerFactoriesOld =
          (ServiceLoader<? extends ConfigurationPropertiesProviderFactory>) load(providerFactoryIfaceOld);
      providerFactoriesOld.forEach(service -> {
        ComponentIdentifier componentIdentifier = service.getSupportedComponentIdentifier();
        if (providerFactoriesMap.containsKey(componentIdentifier)) {
          throw new MuleRuntimeException(createStaticMessage("Multiple configuration providers for component: "
              + componentIdentifier));
        }
        providerFactoriesMap.put(componentIdentifier, service);
      });
    } catch (ClassNotFoundException e) {
      LOGGER
          .debug("Interface 'org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory' not available in classpath, skipping its processing.");
    }

    return providerFactoriesMap;
  }

}
