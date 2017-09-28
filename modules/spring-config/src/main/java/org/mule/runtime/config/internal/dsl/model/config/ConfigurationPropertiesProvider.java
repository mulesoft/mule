/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import java.util.Optional;

/**
 * A provider of configuration attributes.
 * <p>
 * Configuration attributes must be provided upon request since there may be implementations of this interface that do not have a
 * small number of configuration attributes and doing a lookup of the attributes may not be trivial. Such would be the case of a
 * vault implementation of this interface which will probably lookup for values from a remote entity.
 *
 * @since 4.0
 */
public interface ConfigurationPropertiesProvider {

  /**
   * @param configurationAttributeKey the key of the configuration attribute.
   * @return the {@link ConfigurationProperty} associated with the key. May be empty if that key is not present.
   */
  Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey);

  /**
   * @return a meaningful description of the provider. This is used for error reporting.
   */
  String getDescription();

}
