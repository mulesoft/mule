/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Stream.concat;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
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

@IgnoreOnLazyInit
// TODO but enable for tooling!
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
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    Stream<Pair<ParameterModel, ComponentParameterAst>> paramsStream = component.getModel(SourceModel.class)
        .map(sm -> {
          final Stream<Pair<ParameterModel, ComponentParameterAst>> succesCallbackParams = sm.getSuccessCallback()
              .map(scbk -> scbk.getParameterGroupModels().stream()
                  .flatMap(cpgm -> cpgm.getParameterModels().stream()
                      .filter(this::isDoValidation)
                      .map(pm -> new Pair<>(pm, component.getParameter(cpgm.getName(), pm.getName())))))
              .orElse(Stream.empty());

          final Stream<Pair<ParameterModel, ComponentParameterAst>> errorCallbackParams = sm.getErrorCallback()
              .map(ecbk -> ecbk.getParameterGroupModels().stream()
                  .flatMap(cpgm -> cpgm.getParameterModels().stream()
                      .filter(this::isDoValidation)
                      .map(pm -> new Pair<>(pm, component.getParameter(cpgm.getName(), pm.getName())))))
              .orElse(Stream.empty());

          return concat(succesCallbackParams, errorCallbackParams);
        })
        .orElse(Stream.empty());

    paramsStream = concat(paramsStream, component.getModel(ParameterizedModel.class)
        .map(pmzd -> pmzd.getAllParameterModels().stream()
            .filter(this::isDoValidation)
            .map(pm -> new Pair<>(pm, component.getParameter(pm.getName()))))
        .orElse(Stream.empty()));

    return paramsStream
        .map(param -> {
          final Either<String, Object> value = param.getSecond().getValue();
          if (param.getSecond() == null || !value.getValue().isPresent()) {
            return of(create(component, this,
                             format("Element <%s> is missing required parameter '%s'.",
                                    component.getIdentifier().toString(),
                                    param.getFirst().getName())));
          } else if (value.getRight() instanceof ComponentAst) {
            return validate((ComponentAst) value.getRight(), artifact);
          } else {
            return Optional.<ValidationResultItem>empty();
          }
        })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  // componentId presence is already validated by NamedTopLevelElementsHaveName
  protected boolean isDoValidation(ParameterModel pm) {
    return pm.isRequired() && !pm.isComponentId();
  }
}
