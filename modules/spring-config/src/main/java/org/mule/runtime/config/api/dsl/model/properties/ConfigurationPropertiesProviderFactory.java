/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.properties;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;

/**
 * Builds the provider for a custom configuration properties element.
 *
 * @since 4.1
 */
@NoImplement
public interface ConfigurationPropertiesProviderFactory {

  /**
   * @return the component identifier of the supported element.
   */
  ComponentIdentifier getSupportedComponentIdentifier();

  /**
   * Builds a properties provider for each matching configuration element.
   *
   * @param parameters the configuration parameters, after resolving property placeholders
   * @param externalResourceProvider the resource provider for locating files (such as .properties and .yaml)
   * @return the properties provider
   */
  ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters,
                                                 ResourceProvider externalResourceProvider);
}
