/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.topLevelElement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.core.privileged.extension.SingletonModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Elements declared as singletons via the 'SingletonModelProperty' are not repeated.
 */
public class SingletonsAreNotRepeated implements Validation {

  @Override
  public String getName() {
    return "Singletons are not repeated";
  }

  @Override
  public String getDescription() {
    return "Elements declared as singletons via the 'SingletonModelProperty' are not repeated.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return topLevelElement()
        .and(currentElemement(component -> component.getModel(EnrichableModel.class)
            .flatMap(enrchModel -> enrchModel.getModelProperty(SingletonModelProperty.class)
                .map(smp -> isApplicable(smp)))
            .orElse(false)));
  }

  protected boolean isApplicable(SingletonModelProperty smp) {
    return !smp.isAppliesToFile();
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final List<ComponentAst> repeated = artifact.topLevelComponentsStream()
        .filter(comp -> !comp.equals(component))
        .filter(additionalFilter(component))
        .filter(equalsIdentifier(component.getIdentifier()))
        .collect(toList());

    if (repeated.isEmpty()) {
      return empty();
    }

    final List<ComponentAst> allRepeated = new ArrayList<>();
    allRepeated.add(component);
    allRepeated.addAll(repeated);

    return of(create(allRepeated, this, "The configuration element '" + component.getIdentifier() + "' can only appear once."));
  }

  protected Predicate<? super ComponentAst> additionalFilter(ComponentAst component) {
    return comp -> true;
  }

}
