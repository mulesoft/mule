/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.properties;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.internal.dsl.model.DefaultConfigurationParameters;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

import java.util.function.UnaryOperator;

public final class ConfigurationPropertiesProviderFactoryUtils {

  public static ConfigurationParameters resolveConfigurationParameters(DefaultConfigurationParameters.Builder configurationParametersBuilder,
                                                                       ComponentAst component,
                                                                       UnaryOperator<String> localResolver) {
    component.getModel(ParameterizedModel.class)
        .ifPresent(pmzd -> pmzd.getParameterGroupModels()
            .forEach(pmg -> {
              if (pmg.isShowInDsl()) {
                DslElementSyntax childSyntax =
                    component.getGenerationInformation().getSyntax().get().getChild(pmg.getName()).get();
                ComponentIdentifier dslGroupIdentifier = ComponentIdentifier.builder().name(childSyntax.getElementName())
                    .namespace(childSyntax.getPrefix())
                    .namespaceUri(childSyntax.getNamespace())
                    .build();

                DefaultConfigurationParameters.Builder dslGroupParametersBuilder = DefaultConfigurationParameters.builder();
                resolveParamGroupConfigurationParameters(dslGroupParametersBuilder, component, pmg, localResolver);
                configurationParametersBuilder.withComplexParameter(dslGroupIdentifier,
                                                                    dslGroupParametersBuilder.build());

              } else {
                resolveParamGroupConfigurationParameters(configurationParametersBuilder, component, pmg, localResolver);
              }
            }));

    return configurationParametersBuilder.build();
  }

  private static void resolveParamGroupConfigurationParameters(DefaultConfigurationParameters.Builder configurationParametersBuilder,
                                                               ComponentAst component, ParameterGroupModel parameterGroup,
                                                               UnaryOperator<String> localResolver) {
    parameterGroup.getParameterModels()
        .forEach(pm -> {
          ComponentParameterAst param = component.getParameter(pm.getName());
          if (param != null) {
            configurationParametersBuilder
                .withSimpleParameter(pm.getName(), localResolver.apply(param.getResolvedRawValue()));
          }
        });
  }

}
