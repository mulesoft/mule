/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
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
import java.util.stream.Stream;

import com.google.common.base.Joiner;

/**
 * Ref: {@link ExclusiveOptionals}.
 */
@IgnoreOnLazyInit(forceDslDeclarationValidation = true)
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
        .map(pmzd -> resolveGroups(pmzd).stream()
            .anyMatch(pmg -> !pmg.getExclusiveParametersModels().isEmpty()))
        .orElse(false));
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return component.getModel(ParameterizedModel.class)
        .map(pmzd -> validate(component, artifact, pmzd))
        .orElse(emptyList());
  }

  protected List<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact, ParameterizedModel pmzd) {
    for (ParameterGroupModel group : resolveGroups(pmzd)) {
      for (ExclusiveParametersModel exclusiveModel : group.getExclusiveParametersModels()) {
        final Collection<String> definedExclusiveParameters = exclusiveModel.getExclusiveParameterNames().stream()
            .filter(exclParamName -> {
              final ComponentParameterAst value = component.getParameter(group.getName(), exclParamName);
              return !(value == null || !value.getValue().getValue().isPresent());
            })
            .collect(toList());

        if (definedExclusiveParameters.isEmpty() && exclusiveModel.isOneRequired()) {
          return singletonList(create(component, this,
                                      format("Element <%s> requires that one of its optional parameters must be set, but all of them are missing. "
                                          + "One of the following must be set: [%s].",
                                             group.getName(),
                                             Joiner.on(", ").join(exclusiveModel.getExclusiveParameterNames()))));
        } else if (definedExclusiveParameters.size() > 1) {
          return singletonList(create(component,
                                      definedExclusiveParameters.stream()
                                          .map(param -> component.getParameter(group.getName(), param))
                                          .collect(toList()),
                                      this,
                                      format("Element <%s>, the following parameters cannot be set at the same time: [%s].",
                                             getModelName(pmzd),
                                             Joiner.on(", ").join(definedExclusiveParameters))));
        }
      }
    }

    return getGroupAndParametersPairs(pmzd)
        .map(groupAndParam -> component.getParameter(groupAndParam.getFirst().getName(),
                                                     groupAndParam.getSecond().getName()))
        .filter(param -> param != null && param.getValue().getRight() instanceof ComponentAst)
        // validate any nested pojos as well...
        .map(param -> validate((ComponentAst) param.getValue().getRight(), artifact))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  protected List<ParameterGroupModel> resolveGroups(ParameterizedModel pmzd) {
    if (pmzd instanceof SourceModel) {
      return concat(concat(
                           ((SourceModel) pmzd).getSuccessCallback()
                               .map(scbk -> scbk.getParameterGroupModels().stream())
                               .orElse(Stream.empty()),
                           ((SourceModel) pmzd).getErrorCallback()
                               .map(scbk -> scbk.getParameterGroupModels().stream())
                               .orElse(Stream.empty())),
                    pmzd.getParameterGroupModels().stream())
          .collect(toList());
    } else {
      return pmzd.getParameterGroupModels();
    }
  }

}
