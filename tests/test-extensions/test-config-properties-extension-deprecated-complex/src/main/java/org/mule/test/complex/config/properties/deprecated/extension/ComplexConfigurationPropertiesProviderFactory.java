/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.complex.config.properties.deprecated.extension;

import static org.mule.test.complex.config.properties.deprecated.extension.ConfigPropertiesExtensionDeprecated.NAMESPACE_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;

import java.util.ArrayList;
import java.util.List;

public class ComplexConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return ComponentIdentifier.builder()
        .namespace(NAMESPACE_PREFIX)
        .name("config").build();
  }

  @Override
  public ComplexConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters,
                                                               ResourceProvider externalResourceProvider) {
    ConfigurationParameters connectionProvider = parameters.getComplexConfigurationParameter(ComponentIdentifier.builder()
        .namespace(NAMESPACE_PREFIX)
        .name("provider-connection").build()).get(0);

    List<String> textsFromComplexParams = new ArrayList<>();

    textsFromComplexParams.add(connectionProvider.getComplexConfigurationParameter(ComponentIdentifier.builder()
        .namespace(NAMESPACE_PREFIX)
        .name("nested-pojo").build()).get(0)
        .getStringParameter("textValue"));

    connectionProvider.getComplexConfigurationParameter(ComponentIdentifier.builder()
        .namespace(NAMESPACE_PREFIX)
        .name("listed-texts").build()).get(0)
        .getComplexConfigurationParameter(ComponentIdentifier.builder()
            .namespace(NAMESPACE_PREFIX)
            .name("listed-text").build())
        .forEach(listed -> textsFromComplexParams.add(listed.getStringParameter("value")));

    connectionProvider.getComplexConfigurationParameter(ComponentIdentifier.builder()
        .namespace(NAMESPACE_PREFIX)
        .name("listed-pojos").build()).get(0)
        .getComplexConfigurationParameter(ComponentIdentifier.builder()
            .namespace(NAMESPACE_PREFIX)
            .name("some-pojo").build())
        .forEach(listed -> textsFromComplexParams.add(listed.getStringParameter("textValue")));

    connectionProvider.getComplexConfigurationParameter(ComponentIdentifier.builder()
        .namespace(NAMESPACE_PREFIX)
        .name("mapped-pojos").build()).get(0)
        .getComplexConfigurationParameter(ComponentIdentifier.builder()
            .namespace(NAMESPACE_PREFIX)
            .name("mapped-pojo").build())
        .forEach(mappedEnty -> mappedEnty.getComplexConfigurationParameter(ComponentIdentifier.builder()
            .namespace(NAMESPACE_PREFIX)
            .name("some-pojo").build())
            .forEach(mappedValue -> textsFromComplexParams.add(mappedValue.getStringParameter("textValue"))));

    return new ComplexConfigurationPropertiesProvider(textsFromComplexParams);
  }

}
