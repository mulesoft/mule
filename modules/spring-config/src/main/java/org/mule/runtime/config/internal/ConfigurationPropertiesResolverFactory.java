/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Optional.empty;

import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.StaticConfigurationPropertiesProvider;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;

import java.util.Map;

public class ConfigurationPropertiesResolverFactory {

  public static ParsingPropertyResolver createConfigurationPropertiesResolver(Map<String, String> artifactProperties) {
    DefaultConfigurationPropertiesResolver resolver =
        new DefaultConfigurationPropertiesResolver(empty(), new StaticConfigurationPropertiesProvider(artifactProperties));

    return propertyKey -> (String) resolver.resolveValue(propertyKey);
  }
}
