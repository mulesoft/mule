/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.properties;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.config.internal.dsl.model.config.CompositeConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationProperty;
import org.mule.runtime.config.internal.dsl.model.config.EnvironmentPropertiesConfigurationProvider;
import org.mule.runtime.config.internal.dsl.model.config.FileConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.GlobalPropertyConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.MapConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Builder of {@link ConfigurationPropertiesResolver} with the full hierarchy, considering the properties to be used. The
 * hierarchy to consider is: 1. Deployment Properties 2. System Properties 3. Environment Properties 4. Application Properties
 * (this includes file properties) 5. Global (Default) Properties This means that (for example) if we have a property 'hello' both
 * in System Properties level and Application Properties level, then the value to be use is going to be the System Properties one.
 * Also, lower-hierarchy properties can depend their values on higher level properties (e.g. an application property can depend
 * its value on a system property)
 */
public class ConfigurationPropertiesHierarchyBuilder {

  private Optional<ConfigurationPropertiesProvider> deploymentProperties = empty();
  private Optional<ConfigurationPropertiesProvider> systemProperties = empty();
  private Optional<ConfigurationPropertiesProvider> environmentProperties = empty();
  private Optional<ConfigurationPropertiesProvider> fileProperties = empty();
  private List<ConfigurationPropertiesProvider> appProperties = new ArrayList<>();
  private Supplier<Map<String, ConfigurationProperty>> globalPropertiesSupplier = HashMap::new;
  private Optional<ConfigurationPropertiesProvider> domainResolver = empty();

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
   * @param resourceProvider
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withPropertiesFile(ResourceProvider resourceProvider) {
    this.fileProperties = of(new FileConfigurationPropertiesProvider(resourceProvider, "External Files"));
    return this;
  }

  /**
   * Sets a {@link ConfigurationPropertiesProvider} to use as application property, gotten from a custom provider (such as the
   * Secure Configuration Properties) or properties set with the configuration-properties tag.
   * 
   * @param provider
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withApplicationProperties(ConfigurationPropertiesProvider provider) {
    this.appProperties.add(provider);
    return this;
  }

  /**
   * Sets a supplier to retrieve the Global Properties from.
   * 
   * @param supplier
   * @return this builder.
   */
  public ConfigurationPropertiesHierarchyBuilder withGlobalPropertiesSupplier(Supplier<Map<String, ConfigurationProperty>> supplier) {
    this.globalPropertiesSupplier = supplier;
    return this;
  }

  /**
   * Sets to consider the domain properties in the hierarchy, setting a previously resolved {@link ConfigurationProperty} to
   * reoslve.
   * 
   * @param domainProperties
   * @return
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
    this.deploymentProperties = empty();
    this.environmentProperties = empty();
    this.systemProperties = empty();
    return this;
  }

  private void addToHierarchy(List<DefaultConfigurationPropertiesResolver> hierarchy,
                              ConfigurationPropertiesProvider newProvider) {
    Optional<ConfigurationPropertiesResolver> nextResolver = hierarchy.isEmpty() ? empty() : of(hierarchy.get(hierarchy.size() - 1));
    hierarchy.add(new DefaultConfigurationPropertiesResolver(nextResolver, newProvider));
  }


  /**
   * @return the built {@link ConfigurationPropertiesResolver} that includes the complete hierarchy with the defined resolvers.
   */
  public ConfigurationPropertiesResolver build() {
    List<DefaultConfigurationPropertiesResolver> hierarchy = new ArrayList<>();

    addToHierarchy(hierarchy, new GlobalPropertyConfigurationPropertiesProvider(globalPropertiesSupplier));
    domainResolver.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    if (!appProperties.isEmpty()) {
      addToHierarchy(hierarchy, new CompositeConfigurationPropertiesProvider(appProperties));
    }
    fileProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    environmentProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    systemProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));
    deploymentProperties.ifPresent(provider -> addToHierarchy(hierarchy, provider));

    DefaultConfigurationPropertiesResolver lastResolver = hierarchy.get(hierarchy.size() - 1);

    lastResolver.setAsRootResolver();

    return lastResolver;
  }
}
