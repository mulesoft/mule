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
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.config.internal.dsl.model.config.*;
import org.mule.runtime.properties.api.ConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.*;
import java.util.function.Supplier;


public class ConfigurationPropertiesBuilder {

  private Optional<ConfigurationPropertiesProvider> deploymentProperties = empty();
  private Optional<ConfigurationPropertiesProvider> systemProperties = empty();
  private Optional<ConfigurationPropertiesProvider> environmentProperties = empty();
  private Optional<ConfigurationPropertiesProvider> fileProperties = empty();
  private List<ConfigurationPropertiesProvider> appProperties = new ArrayList<>();
  private Map<String, ConfigurationProperty> globalProperties = new HashMap<>();
  private Supplier<Map<String, ConfigurationProperty>> globalPropertiesSupplier = () -> globalProperties;
  private Optional<ConfigurationPropertiesProvider> domainResolver = empty();

  public ConfigurationPropertiesBuilder withDeploymentProperties(Map<String, String> properties) {
    if (!properties.isEmpty()) {
      this.deploymentProperties = of(new MapConfigurationPropertiesProvider(properties, "Deployment properties"));
    }
    return this;
  }

  public ConfigurationPropertiesBuilder withSystemProperties() {
    this.systemProperties = of(new SystemPropertiesConfigurationProvider());
    return this;
  }

  public ConfigurationPropertiesBuilder withEnvironmentProperties() {
    this.environmentProperties = of(new EnvironmentPropertiesConfigurationProvider());
    return this;
  }

  public ConfigurationPropertiesBuilder withPropertiesFile(ResourceProvider resourceProvider) {
    this.fileProperties = of(new FileConfigurationPropertiesProvider(resourceProvider, "External Files"));
    return this;
  }

  public ConfigurationPropertiesBuilder withApplicationProperties(ConfigurationPropertiesProvider provider) {
    this.appProperties.add(provider);
    return this;
  }

  public ConfigurationPropertiesBuilder withGlobalProperties(String name, ConfigurationProperty value) {
    this.globalProperties.put(name, value);
    return this;
  }

  public ConfigurationPropertiesBuilder withGlobalPropertiesSupplier(Supplier<Map<String, ConfigurationProperty>> supplier) {
    this.globalPropertiesSupplier = supplier;
    return this;
  }

  public ConfigurationPropertiesBuilder withDomainPropertiesResolver(ConfigurationProperties domainResolver) {
    this.domainResolver = of(new ConfigurationPropertiesProvider() {

      @Override
      public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
        return domainResolver.resolveProperty(configurationAttributeKey)
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
    Optional<ConfigurationPropertiesResolver> parent = hierarchy.isEmpty() ? empty() : of(hierarchy.get(hierarchy.size() - 1));
    hierarchy.add(new DefaultConfigurationPropertiesResolver(parent, newProvider));
  }

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
