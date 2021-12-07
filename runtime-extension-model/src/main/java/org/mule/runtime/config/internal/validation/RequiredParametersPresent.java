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
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

@IgnoreOnLazyInit(forceDslDeclarationValidation = true)
public class RequiredParametersPresent implements Validation {

  @Override
  public String getName() {
    return "Required parameters are present.";
  }

  @Override
  public String getDescription() {
    return "Required parameters are present.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> comp.getModel(ParameterizedModel.class)
        .map(pmzd -> pmzd.getParameterGroupModels().stream()
            .flatMap(pmg -> pmg.getParameterModels().stream())
            .anyMatch(this::isDoValidation))
        .orElse(false));
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return requiredParamsStream(component)
        .map(param -> {
          if (param.getSecond() == null || !param.getSecond().getValueOrResolutionError().getValue().isPresent()) {
            return of(create(component, param.getSecond(), this,
                             format("Element <%s> is missing required parameter '%s'.",
                                    component.getIdentifier().toString(),
                                    param.getFirst().getName())));
          } else if (param.getSecond().getValueOrResolutionError().isRight()
              && param.getSecond().getValueOrResolutionError().getRight().getRight() instanceof ComponentAst) {
            // validate any nested pojos as well...
            return validate((ComponentAst) param.getSecond().getValueOrResolutionError().getRight().getRight(), artifact);
          } else {
            return Optional.<ValidationResultItem>empty();
          }
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  /**
   * Resolve the provided value for every required param to iterate and check.
   */
  private Stream<Pair<ParameterModel, ComponentParameterAst>> requiredParamsStream(ComponentAst component) {
    Stream<Pair<ParameterModel, ComponentParameterAst>> paramsStream = component.getModel(SourceModel.class)
        .map(sm -> {
          final Stream<Pair<ParameterModel, ComponentParameterAst>> succesCallbackParams = sm.getSuccessCallback()
              .map(scbk -> requiredSourceCallbackParameters(component, scbk))
              .orElse(Stream.empty());

          final Stream<Pair<ParameterModel, ComponentParameterAst>> errorCallbackParams = sm.getErrorCallback()
              .map(ecbk -> requiredSourceCallbackParameters(component, ecbk))
              .orElse(Stream.empty());

          return concat(succesCallbackParams, errorCallbackParams);
        })
        .orElse(Stream.empty());

    return concat(paramsStream, component.getModel(ParameterizedModel.class)
        .map(pmzd -> getGroupAndParametersPairs(pmzd)
            .filter(groupAndParameter -> isDoValidation(groupAndParameter.getSecond()))
            .map(groupAndParameter -> new Pair<>(groupAndParameter.getSecond(),
                                                 component.getParameter(groupAndParameter.getFirst().getName(),
                                                                        groupAndParameter.getSecond().getName()))))
        .orElse(Stream.empty()));
  }

  private Stream<Pair<ParameterModel, ComponentParameterAst>> requiredSourceCallbackParameters(ComponentAst component,
                                                                                               SourceCallbackModel ecbk) {
    return ecbk.getParameterGroupModels().stream()
        .flatMap(cpgm -> cpgm.getParameterModels().stream()
            .filter(this::isDoValidation)
            .map(pm -> new Pair<>(pm, component.getParameter(cpgm.getName(), pm.getName()))));
  }

  protected boolean isDoValidation(ParameterModel pm) {
    // componentId presence is already validated by NamedTopLevelElementsHaveName
    return pm.isRequired() && !pm.isComponentId();
  }
}
