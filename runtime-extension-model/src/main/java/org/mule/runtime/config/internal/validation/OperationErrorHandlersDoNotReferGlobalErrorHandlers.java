/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.util.stream.Stream.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Error handlers within a reusable operation can't have references to global error handlers, neither use the default one, since
 * it's also global.
 *
 * @since 4.5
 */
public class OperationErrorHandlersDoNotReferGlobalErrorHandlers implements Validation {

  private static final ComponentIdentifier OPERATION_BODY_IDENTIFIER = builder().namespace("operation").name("body").build();
  private static final ComponentIdentifier TRY_SCOPE_IDENTIFIER = builder().namespace("mule").name("try").build();

  @Override
  public String getName() {
    return "Operation error handlers don't refer global error handlers";
  }

  @Override
  public String getDescription() {
    return "Error handlers within a reusable operation can't have references to global error handlers, neither use the default one, since it's also global.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(OPERATION_BODY_IDENTIFIER));
  }

  @Override
  public List<ValidationResultItem> validateMany(ComponentAst component, ArtifactAst artifact) {
    return component.recursiveStream().filter(this::isTryScope).flatMap(this::validateTryScope)
        .collect(Collectors.toList());
  }

  private Stream<ValidationResultItem> validateTryScope(ComponentAst tryAst) {
    Optional<ComponentAst> errorHandlerOpt = tryAst.directChildrenStreamByIdentifier("mule", "error-handler").findFirst();
    if (!errorHandlerOpt.isPresent()) {
      return of(create(tryAst, this,
                       "Try scopes within a reusable operation can't use the default error handler because it's global. You have to specify an error handler."));
    }

    ComponentAst errorHandlerAst = errorHandlerOpt.get();
    ComponentParameterAst referenceParameter = errorHandlerAst.getParameter(DEFAULT_GROUP_NAME, "ref");
    if (referenceParameter.getValue().getValue().isPresent()) {
      return of(create(errorHandlerAst, referenceParameter, this,
                       "Error handlers within a reusable operation can't have references to global ones."));
    }

    if (errorHandlerAst.directChildren().isEmpty()) {
      return of(create(errorHandlerAst, this,
                       "The error handler section of a try within a reusable operation must have at least one error handler"));
    }

    return Stream.empty();
  }

  private boolean isTryScope(ComponentAst componentAst) {
    return TRY_SCOPE_IDENTIFIER.equals(componentAst.getIdentifier());
  }
}
