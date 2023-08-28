/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.util.NameValidationUtil.verifyStringDoesNotContainsReservedCharacters;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.topLevelElement;
import static org.mule.runtime.ast.api.util.MuleAstUtils.hasPropertyPlaceholder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 'name' attribute in top-level elements only have valid characters.
 */
public class NameHasValidCharacters implements Validation {

  @Override
  public String getName() {
    return "Names have valid characters";
  }

  @Override
  public String getDescription() {
    return "'name' attribute in top-level elements only have valid characters.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return topLevelElement()
        .and(currentElemement(component -> component.getComponentId().isPresent()));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final String nameAttributeValue = component.getComponentId().get();

    try {
      verifyStringDoesNotContainsReservedCharacters(nameAttributeValue);
      return empty();
    } catch (IllegalArgumentException e) {
      return of(create(component, this,
                       "Invalid global element name '" + nameAttributeValue + "'. Problem is: " + e.getMessage(),
                       hasPropertyPlaceholder(nameAttributeValue)));
    }
  }
}
