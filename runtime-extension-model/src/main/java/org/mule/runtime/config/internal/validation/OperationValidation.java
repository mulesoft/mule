/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


public abstract class OperationValidation implements Validation {

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
