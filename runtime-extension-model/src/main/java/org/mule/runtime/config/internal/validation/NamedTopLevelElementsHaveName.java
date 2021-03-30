/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.topLevelElement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 'name' attribute in top-level elements is present.
 */
public class NamedTopLevelElementsHaveName implements Validation {

  @Override
  public String getName() {
    return "Named Top-Level elements have name";
  }

  @Override
  public String getDescription() {
    return "'name' attribute in top-level elements is present";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return topLevelElement()
        // We have a name, good to go!
        .and(currentElemement(topLevelComponent -> !topLevelComponent.getComponentId().isPresent()))
        // Skip the validation if there is no model available. This situation happens in some test cases.
        .and(currentElemement(topLevelComponent -> topLevelComponent.getModel(Object.class).isPresent()))
        // APP_CONFIGs need not be referenced by name, so this exception is ok
        // Not making this exception would break backwards compatibility in spring:security-manager
        .and(currentElemement(topLevelComponent -> !topLevelComponent.getModel(HasStereotypeModel.class)
            .map(sm -> sm.getStereotype().isAssignableTo(APP_CONFIG))
            .orElse(false)));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst topLevelComponent, ArtifactAst artifact) {
    if (!topLevelComponent.getModel(ConstructModel.class).isPresent()
        || topLevelComponent.getModel(ParameterizedModel.class)
            .map(pmzd -> pmzd.getAllParameterModels().stream()
                .anyMatch(ParameterModel::isComponentId))
            .orElse(false)) {
      final ComponentIdentifier identifier = topLevelComponent.getIdentifier();
      return of(create(topLevelComponent, this, format("Global element '%s' does not provide a name attribute.",
                                                       identifier.toString())));
    }

    return empty();
  }

}
