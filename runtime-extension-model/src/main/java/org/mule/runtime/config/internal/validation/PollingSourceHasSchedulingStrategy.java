/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getGroupAndParametersPairs;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@IgnoreOnLazyInit
public class PollingSourceHasSchedulingStrategy implements Validation {

  @Override
  public String getName() {
    return "Polling source has scheduling strategy set";
  }

  @Override
  public String getDescription() {
    return "Polling source has scheduling strategy set";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.getModel(SourceModel.class)
        .flatMap(srcModel -> srcModel.getAllParameterModels().stream()
            .filter(pm -> isScheduler(pm.getType()))
            .findAny())
        .isPresent());
  }

  private boolean isScheduler(MetadataType type) {
    return getTypeId(type)
        .filter(typeId -> SchedulingStrategy.class.getName().equals(typeId))
        .isPresent();
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final Pair<ParameterGroupModel, ParameterModel> schedulingStrategyParamModel = component.getModel(SourceModel.class)
        .flatMap(srcModel -> getGroupAndParametersPairs(srcModel)
            .filter(pm -> isScheduler(pm.getSecond().getType()))
            .findAny())
        .get();

    if (component
        .getParameter(schedulingStrategyParamModel.getFirst().getName(), schedulingStrategyParamModel.getSecond().getName())
        .getValue().getRight() == null) {
      return of(ValidationResultItem.create(component, this, "The scheduling strategy has not been configured."));
    } else {
      return empty();
    }
  }

}
