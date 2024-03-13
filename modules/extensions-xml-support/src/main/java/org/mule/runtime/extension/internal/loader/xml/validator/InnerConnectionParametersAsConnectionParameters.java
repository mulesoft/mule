/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml.validator;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONNECTION;

import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.ast.property.GlobalElementComponentModelModelProperty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InnerConnectionParametersAsConnectionParameters implements ExtensionModelValidator {

  private static final Pattern VARS_EXPRESSION_PATTERN = compile("^#\\[vars\\.(\\w+)\\]$");

  @Override
  public void validate(ExtensionModel extensionModel, ProblemsReporter problemsReporter) {
    extensionModel.getModelProperty(GlobalElementComponentModelModelProperty.class)
        .ifPresent(mp -> mp.getGlobalElements()
            .stream()
            .forEach(globalElement -> globalElement.recursiveStream()
                .filter(comp -> comp.getComponentType().equals(CONNECTION))
                .forEach(connectionProvider -> {
                  List<String> connectionProperties = resolveConnectionProperties(connectionProvider);

                  extensionModel.getConfigurationModels()
                      .forEach(configModel -> {
                        final Set<String> configParamsUsedForConnection =
                            resolveConfigParamsUsedForConnection(configModel,
                                                                 connectionProperties,
                                                                 resolveConnectionProviderParams(configModel));

                        if (!configParamsUsedForConnection.isEmpty()) {
                          problemsReporter
                              .addWarning(new Problem(extensionModel.getConfigurationModels().get(0),
                                                      "Connection provider for '"
                                                          + globalElement.getComponentId().orElse(null)
                                                          + "' uses properties " + configParamsUsedForConnection
                                                          + " that are defined at the config level, not within <connection>"));
                        }
                      });
                })));
  }

  private List<String> resolveConnectionProperties(final ComponentAst connectionProvider) {
    return connectionProvider.recursiveStream()
        .flatMap(comp -> comp.getParameters().stream())
        .filter(p -> p.getRawValue() != null)
        .map(p -> {
          final Matcher matcher = VARS_EXPRESSION_PATTERN.matcher(p.getRawValue());
          if (matcher.matches()) {
            return of(matcher.group(1));
          }

          return Optional.<String>empty();
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private Set<String> resolveConnectionProviderParams(final ConfigurationModel configModel) {
    return configModel.getConnectionProviders()
        .stream()
        .map(pm -> pm.getName())
        .collect(toSet());
  }

  private Set<String> resolveConfigParamsUsedForConnection(final ConfigurationModel configModel,
                                                           final List<String> connectionProperties,
                                                           final Set<String> connectionProviderParams) {
    return configModel.getAllParameterModels()
        .stream()
        .map(pm -> pm.getName())
        .filter(name -> !connectionProviderParams.contains(name))
        .filter(connectionProperties::contains)
        .collect(toSet());
  }

}
