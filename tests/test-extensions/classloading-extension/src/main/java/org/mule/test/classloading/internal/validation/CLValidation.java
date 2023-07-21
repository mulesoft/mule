/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading.internal.validation;

import static java.util.Optional.of;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CLValidation implements Validation {

  @Override
  public String getName() {
    return "Contributed CL Validation";
  }

  @Override
  public String getDescription() {
    return "Verify the validations providing mechanism for extensions.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  public String getComponentName() {
    return "config";
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> comp.getIdentifier().getName().equals("invalid-config"));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    return of(create(component, this, "'invalid-config' is invalid"));
  }

}
