/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.properties;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.CompositeConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationProperty;
import org.mule.runtime.config.internal.model.dsl.config.EnvironmentPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.model.dsl.config.FileConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.model.dsl.config.GlobalPropertyConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.model.dsl.config.MapConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.model.dsl.config.PropertyNotFoundException;
import org.mule.runtime.config.internal.model.dsl.config.StaticConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Builder of {@link ConfigurationPropertiesResolver} with the full hierarchy, considering the properties to be used. The
 * hierarchy to consider is:
 * <ol>
 * <li>Deployment Properties</li>
 * <li>System Properties</li>
 * <li>Environment Properties</li>
 * <li>Application Properties (this includes file properties)</li>
 * <li>Global (Default) Properties</li>
 * </ol>
 * This means that (for example) if we have a property 'hello' both in System Properties level and Application Properties level,
 * then the value to be use is going to be the System Properties one. Also, lower-hierarchy properties can depend their values on
 * higher level properties (e.g. an application property can depend its value on a system property)
 *
 * @since 4.5
 */
public class ConfigurationPropertiesHierarchyBuilder {

  // Every type of property could be set or not, upon requirements of usages
  // if a type of property is not used, then it won't be added to the hierarchy.
  private Optional<ConfigurationPropertiesProvider> deploymentProperties = empty();
  private Optional<ConfigurationPropertiesProvider> systemProperties = empty();
  private Optional<ConfigurationPropertiesProvider> environmentProperties = empty();
  private Optional<ConfigurationPropertiesProvider> fileProperties = empty();
  private Optional<ConfigurationPropertiesProvider> domainResolver = empty();
  private final List<ConfigurationPropertiesProvider> appProperties = new ArrayList<>();
  private Supplier<Map<String, ConfigurationProperty>> globalPropertiesSupplier = HashMap::new;
  private boolean failuresIfNotPresent = true;

  /**
   * @param properties the deployment properties to consider, as a map.
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withDeploymentProperties(Map<String, String> properties) {
    if (!properties.isEmpty()) {
      this.deploymentProperties = of(new MapConfigurationPropertiesProvider(properties, "Deployment properties"));
    }
    return this;
  }

  /**
   * Sets that system properties added to the JVM are considered in the hierarchy.
   *
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withSystemProperties() {
    this.systemProperties = of(new SystemPropertiesConfigurationProvider());
    return this;
  }

  /**
   * Sets that environment properties (from OS) should be considered in the hierarchy.
   *
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withEnvironmentProperties() {
    this.environmentProperties = of(new EnvironmentPropertiesConfigurationProvider());
    return this;
  }

  /**
   * Sets an external resource provider to get the file content to then use a File Configuration Property.
   *
   * @param resourceProvider the {@link ResourceProvider} to use to read the files to use when resolving a file-value property.
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withPropertiesFile(ResourceProvider resourceProvider) {
    this.fileProperties = of(new FileConfigurationPropertiesProvider(resourceProvider, "External Files"));
    return this;
  }

  /**
   * Sets an external resource provider classloader to get the file content to then use a File Configuration Property.
   *
   * @param classLoader the {@link ClassLoader} to use to read the files to use when resolving a file-value property.
   * @return this builder.
   * @since 4.8
   */
  public ConfigurationPropertiesHierarchyBuilder withPropertiesFile(ClassLoader classLoader) {
    this.fileProperties =
        of(new FileConfigurationPropertiesProvider(new ClassLoaderResourceProvider(classLoader), "External Files"));
    return this;
  }

  /**
   * Sets a {@link ConfigurationPropertiesProvider} to use as application property, gotten from a custom provider (such as the
   * Secure Configuration Properties) or properties set with the configuration-properties tag.
   *
   * @param provider the {@link ConfigurationPropertiesProvider} to add to the hierarchy as Application Properties.
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withApplicationProperties(ConfigurationPropertiesProvider provider) {
    this.appProperties.add(provider);
    return this;
  }

  /**
   * Creates and sets a {@link ConfigurationPropertiesProvider} to use as application property.
   *
   * @param artifactProperties the properties to add to the hierarchy as Application Properties.
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withApplicationProperties(Map<String, String> artifactProperties) {
    this.appProperties.add(new StaticConfigurationPropertiesProvider(artifactProperties));
    return this;
  }

  /**
   * Sets a supplier to retrieve the Global Properties from.
   *
   * @param globalPropertiesSupplier a {@link Supplier} of a {@link Map} of properties, to be used as Global/Default properties.
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withGlobalPropertiesSupplier(Supplier<Map<String, ConfigurationProperty>> globalPropertiesSupplier) {
    this.globalPropertiesSupplier = globalPropertiesSupplier;
    return this;
  }

  /**
   * Sets to consider the domain properties in the hierarchy, setting a previously resolved {@link ConfigurationProperty} to
   * reoslve.
   *
   * @param domainProperties the {@link ConfigurationProperties} previously generated for the domain, to be used in this
   *                         hierarchy.
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withDomainPropertiesResolver(ConfigurationProperties domainProperties) {
    this.domainResolver = of(new ConfigurationPropertiesProvider() {

      @Override
      public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
        return domainProperties.resolveProperty(configurationAttributeKey)
            .map(value -> new DefaultConfigurationProperty(domainResolver, configurationAttributeKey, value));
      }

      @Override
      public String getDescription() {
        return "Domain properties";
      }
    });
    return this;
  }

  /**
   * Set that the {@link ConfigurationPropertiesResolver} to be built won't fail in case of resolving a property doesn't exist.
   * Instead of throwing a {@link PropertyNotFoundException}, it will return null.
   *
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withoutFailuresIfPropertyNotPresent() {
    this.failuresIfNotPresent = false;
    return this;
  }

  private void addToHierarchy(ArrayDeque<DefaultConfigurationPropertiesResolver> hierarchy,
                              ConfigurationPropertiesProvider newProvider) {
    Optional<ConfigurationPropertiesResolver> nextResolver = hierarchy.isEmpty() ? empty() : of(hierarchy.peek());
    hierarchy.push(new DefaultConfigurationPropertiesResolver(nextResolver, newProvider, failuresIfNotPresent));
  }

  /**
   * @return the built {@link ConfigurationPropertiesResolver} that includes the complete hierarchy with the defined resolvers.
   */
  public ConfigurationPropertiesResolver build() {
    ArrayDeque<DefaultConfigurationPropertiesResolver> hierarchy = new ArrayDeque<>();

    addToHierarchy(hierarchy, new GlobalPropertyConfigurationPropertiesProvider(globalPropertiesSupplier));
    domainResolver.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    if (!appProperties.isEmpty()) {
      addToHierarchy(hierarchy, new CompositeConfigurationPropertiesProvider(appProperties));
    }
    fileProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    environmentProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    systemProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    deploymentProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));

    DefaultConfigurationPropertiesResolver lastResolver = hierarchy.peek();
    lastResolver.setAsRootResolver();
    return lastResolver;
  }

  /**
   * @return the built {@link ConfigurationPropertiesResolver} that includes the complete hierarchy with the defined resolvers.
   *         This hierarchy is a broken/legacy hierarchy (with deployment properties at the bottom) and without the circular
   *         resolution. This is intended to be used only by applications previous to 4.3.0.
   * @deprecated since 4.5. Use {@link ConfigurationPropertiesHierarchyBuilder#build} instead.
   */
  @Deprecated
  public ConfigurationPropertiesResolver buildLegacyHierarchy() {
    ArrayDeque<DefaultConfigurationPropertiesResolver> hierarchy = new ArrayDeque<>();

    deploymentProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    addToHierarchy(hierarchy, new GlobalPropertyConfigurationPropertiesProvider(globalPropertiesSupplier));
    domainResolver.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    if (!appProperties.isEmpty()) {
      addToHierarchy(hierarchy, new CompositeConfigurationPropertiesProvider(appProperties));
    }
    fileProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    environmentProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    systemProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));

    DefaultConfigurationPropertiesResolver lastResolver = hierarchy.peek();
    lastResolver.setAsRootResolver();
    return lastResolver;
  }
}
