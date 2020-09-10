/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.properties;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.internal.dsl.model.DefaultConfigurationParameters;

import java.util.function.UnaryOperator;

public final class ConfigurationPropertiesProviderFactoryUtils {

  public static ConfigurationParameters resolveConfigurationParameters(DefaultConfigurationParameters.Builder configurationParametersBuilder,
                                                                       ComponentAst component,
                                                                       UnaryOperator<String> localResolver) {

    component.getParameters()
        .forEach(param -> configurationParametersBuilder
            .withSimpleParameter(param.getModel().getName(), localResolver.apply(param.getRawValue())));

    component
        .directChildrenStream()
        .forEach(child -> {
          DefaultConfigurationParameters.Builder childParametersBuilder = DefaultConfigurationParameters.builder();
          configurationParametersBuilder.withComplexParameter(child.getIdentifier(),
                                                              resolveConfigurationParameters(childParametersBuilder, child,
                                                                                             localResolver));
        });

    return configurationParametersBuilder.build();
  }

}
