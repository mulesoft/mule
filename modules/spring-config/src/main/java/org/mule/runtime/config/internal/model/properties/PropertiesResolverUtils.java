/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.properties;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.AbstractComponent.ANNOTATION_NAME;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.model.ApplicationModel.GLOBAL_PROPERTY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.dsl.model.config.CompositeConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationProperty;
import org.mule.runtime.config.internal.dsl.model.config.EnvironmentPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.dsl.model.config.FileConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.GlobalPropertyConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.MapConfigurationPropertiesProvider;
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

import javax.xml.namespace.QName;

/**
 * Provides a common set of utilities for handling property resolvers for Mule artifacts.
 *
 */
public class PropertiesResolverUtils {

  private PropertiesResolverUtils() {
    // Nothing to do
  }

  // TODO MULE-18786 refactor this
  public static PropertiesResolverConfigurationProperties createConfigurationAttributeResolver(ArtifactAst artifactAst,
                                                                                               Optional<ConfigurationProperties> parentConfigurationProperties,
                                                                                               Map<String, String> deploymentProperties,
                                                                                               ResourceProvider externalResourceProvider) {
    ConfigurationPropertiesProvider deploymentPropertiesConfigurationProperties = null;
    if (!deploymentProperties.isEmpty()) {
      deploymentPropertiesConfigurationProperties =
          new MapConfigurationPropertiesProvider(deploymentProperties, "Deployment properties");
    }

    EnvironmentPropertiesConfigurationProvider environmentPropertiesConfigurationProvider =
        new EnvironmentPropertiesConfigurationProvider();
    ConfigurationPropertiesProvider globalPropertiesConfigurationAttributeProvider =
        createProviderFromGlobalProperties(artifactAst);

    DefaultConfigurationPropertiesResolver environmentPropertiesConfigurationPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(empty(), environmentPropertiesConfigurationProvider);

    DefaultConfigurationPropertiesResolver parentLocalResolver;
    if (deploymentPropertiesConfigurationProperties != null) {
      parentLocalResolver = new DefaultConfigurationPropertiesResolver(of(environmentPropertiesConfigurationPropertiesResolver),
                                                                       deploymentPropertiesConfigurationProperties);
    } else {
      parentLocalResolver = environmentPropertiesConfigurationPropertiesResolver;
    }

    DefaultConfigurationPropertiesResolver localResolver =
        new DefaultConfigurationPropertiesResolver(of(new DefaultConfigurationPropertiesResolver(of(parentLocalResolver),
                                                                                                 globalPropertiesConfigurationAttributeProvider)),
                                                   environmentPropertiesConfigurationProvider);
    localResolver.setRootResolver(parentLocalResolver);

    artifactAst.updatePropertiesResolver(localResolver);
    List<ConfigurationPropertiesProvider> configConfigurationPropertiesProviders =
        getConfigurationPropertiesProvidersFromComponents(artifactAst, externalResourceProvider, localResolver);
    FileConfigurationPropertiesProvider externalPropertiesConfigurationProvider =
        new FileConfigurationPropertiesProvider(externalResourceProvider, "External files");

    Optional<ConfigurationPropertiesResolver> parentConfigurationPropertiesResolver = of(localResolver);
    if (parentConfigurationProperties.isPresent()) {
      parentConfigurationPropertiesResolver =
          of(new DefaultConfigurationPropertiesResolver(empty(), new ConfigurationPropertiesProvider() {

            @Override
            public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
              return parentConfigurationProperties.get().resolveProperty(configurationAttributeKey)
                  .map(value -> new DefaultConfigurationProperty(parentConfigurationProperties, configurationAttributeKey,
                                                                 value));
            }

            @Override
            public String getDescription() {
              return "Domain properties";
            }
          }));
    }

    Optional<CompositeConfigurationPropertiesProvider> configurationAttributesProvider = empty();
    if (!configConfigurationPropertiesProviders.isEmpty()) {
      configurationAttributesProvider = of(new CompositeConfigurationPropertiesProvider(configConfigurationPropertiesProviders));
      parentConfigurationPropertiesResolver =
          of(new DefaultConfigurationPropertiesResolver(deploymentPropertiesConfigurationProperties != null
              // deployment properties provider has to go as parent here so we can reference them from configuration properties
              // files
              ? of(new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                              deploymentPropertiesConfigurationProperties))
              : parentConfigurationPropertiesResolver,
                                                        configurationAttributesProvider.get()));
    } else if (deploymentPropertiesConfigurationProperties != null) {
      parentConfigurationPropertiesResolver =
          of(new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                        deploymentPropertiesConfigurationProperties));
    }

    DefaultConfigurationPropertiesResolver globalPropertiesConfigurationPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                   globalPropertiesConfigurationAttributeProvider);

    DefaultConfigurationPropertiesResolver systemPropertiesResolver;
    if (configurationAttributesProvider.isPresent()) {
      DefaultConfigurationPropertiesResolver configurationPropertiesResolver =
          new DefaultConfigurationPropertiesResolver(of(globalPropertiesConfigurationPropertiesResolver),
                                                     configurationAttributesProvider.get());
      systemPropertiesResolver = new DefaultConfigurationPropertiesResolver(of(configurationPropertiesResolver),
                                                                            environmentPropertiesConfigurationProvider);
    } else {
      systemPropertiesResolver = new DefaultConfigurationPropertiesResolver(of(globalPropertiesConfigurationPropertiesResolver),
                                                                            environmentPropertiesConfigurationProvider);
    }

    DefaultConfigurationPropertiesResolver externalPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(deploymentPropertiesConfigurationProperties != null
            // deployment properties provider has to go as parent here so we can reference
            // them from external files
            ? of(new DefaultConfigurationPropertiesResolver(of(systemPropertiesResolver),
                                                            deploymentPropertiesConfigurationProperties))
            : of(systemPropertiesResolver),
                                                   externalPropertiesConfigurationProvider);
    if (deploymentPropertiesConfigurationProperties == null) {
      externalPropertiesResolver.setAsRootResolver();
      return new PropertiesResolverConfigurationProperties(externalPropertiesResolver);
    } else {
      // finally the first configuration properties resolver should be deployment properties as they have precedence over the rest
      DefaultConfigurationPropertiesResolver deploymentPropertiesResolver =
          new DefaultConfigurationPropertiesResolver(of(externalPropertiesResolver), deploymentPropertiesConfigurationProperties);
      deploymentPropertiesResolver.setAsRootResolver();
      return new PropertiesResolverConfigurationProperties(deploymentPropertiesResolver);
    }
  }

  // TODO MULE-18786 refactor this
  public static PropertiesResolverConfigurationProperties createConfigurationAttributeResolver(Optional<ConfigurationProperties> parentConfigurationProperties,
                                                                                               Map<String, String> deploymentProperties,
                                                                                               ResourceProvider externalResourceProvider) {
    ConfigurationPropertiesProvider deploymentPropertiesConfigurationProperties = null;
    if (!deploymentProperties.isEmpty()) {
      deploymentPropertiesConfigurationProperties =
          new MapConfigurationPropertiesProvider(deploymentProperties, "Deployment properties");
    }

    EnvironmentPropertiesConfigurationProvider environmentPropertiesConfigurationProvider =
        new EnvironmentPropertiesConfigurationProvider();

    DefaultConfigurationPropertiesResolver environmentPropertiesConfigurationPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(empty(), environmentPropertiesConfigurationProvider);

    DefaultConfigurationPropertiesResolver parentLocalResolver;
    if (deploymentPropertiesConfigurationProperties != null) {
      parentLocalResolver = new DefaultConfigurationPropertiesResolver(of(environmentPropertiesConfigurationPropertiesResolver),
                                                                       deploymentPropertiesConfigurationProperties);
    } else {
      parentLocalResolver = environmentPropertiesConfigurationPropertiesResolver;
    }

    DefaultConfigurationPropertiesResolver localResolver =
        new DefaultConfigurationPropertiesResolver(of(parentLocalResolver),
                                                   environmentPropertiesConfigurationProvider);
    localResolver.setRootResolver(parentLocalResolver);

    FileConfigurationPropertiesProvider externalPropertiesConfigurationProvider =
        new FileConfigurationPropertiesProvider(externalResourceProvider, "External files");

    Optional<ConfigurationPropertiesResolver> parentConfigurationPropertiesResolver = of(localResolver);
    if (parentConfigurationProperties.isPresent()) {
      parentConfigurationPropertiesResolver =
          of(new DefaultConfigurationPropertiesResolver(empty(), new ConfigurationPropertiesProvider() {

            @Override
            public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
              return parentConfigurationProperties.get().resolveProperty(configurationAttributeKey)
                  .map(value -> new DefaultConfigurationProperty(parentConfigurationProperties, configurationAttributeKey,
                                                                 value));
            }

            @Override
            public String getDescription() {
              return "Domain properties";
            }
          }));
    }

    Optional<CompositeConfigurationPropertiesProvider> configurationAttributesProvider = empty();

    DefaultConfigurationPropertiesResolver systemPropertiesResolver;
    if (configurationAttributesProvider.isPresent()) {
      DefaultConfigurationPropertiesResolver configurationPropertiesResolver =
          new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                     configurationAttributesProvider.get());
      systemPropertiesResolver = new DefaultConfigurationPropertiesResolver(of(configurationPropertiesResolver),
                                                                            environmentPropertiesConfigurationProvider);
    } else {
      systemPropertiesResolver = new DefaultConfigurationPropertiesResolver(parentConfigurationPropertiesResolver,
                                                                            environmentPropertiesConfigurationProvider);
    }

    DefaultConfigurationPropertiesResolver externalPropertiesResolver =
        new DefaultConfigurationPropertiesResolver(deploymentPropertiesConfigurationProperties != null
            // deployment properties provider has to go as parent here so we can reference
            // them from external files
            ? of(new DefaultConfigurationPropertiesResolver(of(systemPropertiesResolver),
                                                            deploymentPropertiesConfigurationProperties))
            : of(systemPropertiesResolver),
                                                   externalPropertiesConfigurationProvider);
    if (deploymentPropertiesConfigurationProperties == null) {
      externalPropertiesResolver.setAsRootResolver();
      return new PropertiesResolverConfigurationProperties(externalPropertiesResolver);
    } else {
      // finally the first configuration properties resolver should be deployment properties as they have precedence over the rest
      DefaultConfigurationPropertiesResolver deploymentPropertiesResolver =
          new DefaultConfigurationPropertiesResolver(of(externalPropertiesResolver), deploymentPropertiesConfigurationProperties);
      deploymentPropertiesResolver.setAsRootResolver();
      return new PropertiesResolverConfigurationProperties(deploymentPropertiesResolver);
    }
  }

  public static ConfigurationPropertiesProvider createProviderFromGlobalProperties(ArtifactAst artifactAst) {
    return new GlobalPropertyConfigurationPropertiesProvider(new LazyValue<>(() -> {
      final Map<String, ConfigurationProperty> globalProperties = new HashMap<>();

      artifactAst.topLevelComponentsStream()
          .filter(comp -> GLOBAL_PROPERTY.equals(comp.getIdentifier().getName()))
          .forEach(comp -> {
            final String key = comp.getParameter("name").getResolvedRawValue();
            final String rawValue = comp.getParameter("value").getRawValue();
            globalProperties.put(key,
                                 new DefaultConfigurationProperty(format("global-property - file: %s - lineNumber %s",
                                                                         comp.getMetadata().getFileName().orElse("(n/a)"),
                                                                         comp.getMetadata().getStartLine().orElse(-1)),
                                                                  key, rawValue));
          });

      return globalProperties;
    }));
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

    ServiceLoader<org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory> providerFactoriesOld =
        load(org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory.class);
    providerFactoriesOld.forEach(service -> {
      ComponentIdentifier componentIdentifier = service.getSupportedComponentIdentifier();
      if (providerFactoriesMap.containsKey(componentIdentifier)) {
        throw new MuleRuntimeException(createStaticMessage("Multiple configuration providers for component: "
            + componentIdentifier));
      }
      providerFactoriesMap.put(componentIdentifier, service);
    });

    return providerFactoriesMap;
  }

}
