/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl.model.properties;


import java.util.Optional;

/**
 * A provider of configuration attributes.
 * <p>
 * Configuration attributes must be provided upon request since there may be implementations of this interface that do not have a
 * small number of configuration attributes and doing a lookup of the attributes may not be trivial. Such would be the case of a
 * vault implementation of this interface which will probably lookup for values from a remote entity.
 *
 * @since 4.0
 *
 * @deprecated since 4.4, use org.mule.runtime.properties.api.ConfigurationPropertiesProvider instead.
 */
@Deprecated
public interface ConfigurationPropertiesProvider extends org.mule.runtime.properties.api.ConfigurationPropertiesProvider {

  /**
   * @param configurationAttributeKey the key of the configuration attribute.
   * @return the {@link ConfigurationProperty} associated with the key. May be empty if that key is not present.
   */
  Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey);

  @Override
  default Optional<? extends org.mule.runtime.properties.api.ConfigurationProperty> provide(String configurationAttributeKey) {
    return getConfigurationProperty(configurationAttributeKey);
  }

  /**
   * @return a meaningful description of the provider. This is used for error reporting.
   */
  @Override
  String getDescription();

}
