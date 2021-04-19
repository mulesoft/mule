/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;

import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.sdk.api.annotation.param.ExclusiveOptionals;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.base.Joiner;

/**
 * Ref: {@link ExclusiveOptionals}.
 */
@IgnoreOnLazyInit
// TODO but enable for tooling!
public class ParameterGroupExclusiveness implements Validation {

  @Override
  public String getName() {
    return "ExclusiveOptionals annotation is honored.";
  }

  @Override
  public String getDescription() {
    return "ExclusiveOptionals annotation is honored.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> comp.getModel(ParameterizedModel.class)
        .map(pmzd -> pmzd.getParameterGroupModels().stream()
            .anyMatch(pmg -> !pmg.getExclusiveParametersModels().isEmpty()))
        .orElse(false));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    return component.getModel(ParameterizedModel.class)
        .flatMap(pmzd -> {
          final List<ParameterGroupModel> groups = pmzd.getParameterGroupModels();
          for (ParameterGroupModel group : groups) {
            for (ExclusiveParametersModel exclusiveModel : group.getExclusiveParametersModels()) {
              final Collection<String> definedExclusiveParameters = exclusiveModel.getExclusiveParameterNames().stream()
                  .filter(exclParamName -> {
                    final ComponentParameterAst value = component.getParameter(exclParamName);
                    return !(value == null || !value.getValue().getValue().isPresent());
                  })
                  .collect(toList());

              if (definedExclusiveParameters.isEmpty() && exclusiveModel.isOneRequired()) {
                return of(create(component, this,
                                 format("Element <%s> requires that one of its optional parameters must be set, but all of them are missing. "
                                     + "One of the following must be set: [%s].",
                                        group.getName(),
                                        Joiner.on(", ").join(exclusiveModel.getExclusiveParameterNames()))));
              } else if (definedExclusiveParameters.size() > 1) {
                // if (model.isPresent()) {
                return of(create(component,
                                 definedExclusiveParameters.stream().map(component::getParameter).collect(toList()),
                                 this,
                                 format("Element <%s>, the following parameters cannot be set at the same time: [%s].",
                                        getModelName(pmzd),
                                        Joiner.on(", ").join(definedExclusiveParameters))));
                // } else {
                // throw new ConfigurationException(createStaticMessage(format("The following parameters cannot be set at the same
                // time: [%s]",
                // Joiner.on(", ").join(definedExclusiveParameters))));
                // }
                // TODO AnonymousGroup??
              }
            }
          }

          return pmzd.getAllParameterModels().stream()
              .map(pm -> component.getParameter(pm.getName()))
              .filter(param -> param != null && param.getValue().getRight() instanceof ComponentAst)
              .map(param -> validate((ComponentAst) param.getValue().getRight(), artifact))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .findFirst();
        });
  }

}
