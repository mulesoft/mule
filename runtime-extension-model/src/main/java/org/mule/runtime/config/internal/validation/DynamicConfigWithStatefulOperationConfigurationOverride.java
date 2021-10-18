/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class DynamicConfigWithStatefulOperationConfigurationOverride implements Validation {

  @Override
  public String getName() {
    return "Dynamic config with stateful operation @ConfigOverride";
  }

  @Override
  public String getDescription() {
    return "Dynamic config with stateful operation @ConfigOverride";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> !getConfigOverrideParams(component).isEmpty());
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    return getConfigOverrideParams(component)
        .stream()
        .filter(parameter -> parameter.getValue().equals(Either.empty()))
        .findFirst()
        .map(parameter -> create(component, parameter, this,
                                 "Component uses a dynamic configuration and defines configuration override parameter '"
                                     + parameter.getModel().getName()
                                     + "' which is assigned on initialization. That combination is not supported. Please use a non dynamic configuration or don't set the parameter."));
  }

  protected List<ComponentParameterAst> getConfigOverrideParams(ComponentAst component) {
    return component.getModel(ParameterizedModel.class)
        .map(pmzd -> pmzd.getParameterGroupModels()
            .stream()
            .flatMap(pmg -> pmg.getParameterModels().stream()
                .filter(ParameterModel::isOverrideFromConfig)
                .map(pm -> component.getParameter(pmg.getName(), pm.getName())))
            .collect(toList()))
        .orElse(emptyList());
  }

}
