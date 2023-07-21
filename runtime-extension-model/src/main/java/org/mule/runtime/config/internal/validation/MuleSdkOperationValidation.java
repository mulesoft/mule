/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.topLevelElement;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.List;
import java.util.function.Predicate;


public abstract class MuleSdkOperationValidation implements Validation {

  protected static final ComponentIdentifier OPERATION_IDENTIFIER = builder().namespace("operation").name("def").build();

  @Override
  public Level getLevel() {
    return Level.ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return topLevelElement().and(currentElemement(component -> component.getIdentifier().equals(OPERATION_IDENTIFIER)));
  }
}
