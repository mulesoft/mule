/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.StaticConfigurationPropertiesProvider;
import org.mule.runtime.dsl.api.xml.parser.ParsingPropertyResolver;

import java.util.Map;

/**
 * Allows to create a basic properties resolver to use when initializing applications for tests.
 * 
 * @since 4.5
 * @deprecated Use only for tests
 */
@Deprecated
public class ConfigurationPropertiesResolverFactory {

  public static ParsingPropertyResolver createConfigurationPropertiesResolver(Map<String, String> artifactProperties) {
    ConfigurationPropertiesResolver resolver = new ConfigurationPropertiesHierarchyBuilder()
        .withApplicationProperties(new StaticConfigurationPropertiesProvider(artifactProperties))
        .build();

    return propertyKey -> (String) resolver.resolveValue(propertyKey);
  }
}
