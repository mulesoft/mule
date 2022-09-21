/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.IdentifierParsingUtils.getNamespace;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;

import static java.lang.String.format;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Operation raise error doesn't specify namespace.
 *
 * @since 4.5
 */
public class OperationRaiseErrorDoesntSpecifyNamespace implements Validation {

  private static final String OPERATION_PREFIX = "operation";
  private static final String RAISE_ERROR = "raise-error";

  private static final ComponentIdentifier OPERATION_RAISE_ERROR_IDENTIFIER =
      builder().namespace(OPERATION_PREFIX).name(RAISE_ERROR).build();

  @Override
  public String getName() {
    return "Operation raise error doesn't specify namespace";
  }

  @Override
  public String getDescription() {
    return "Operation raise error doesn't specify namespace.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(OPERATION_RAISE_ERROR_IDENTIFIER));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst errorTypeParam = getErrorTypeParam(component);
    final String errorTypeString = errorTypeParam.getResolvedRawValue();
    return getNamespace(errorTypeString).map(s -> create(component, errorTypeParam, this,
                                                         format("Operation raise error component (%s) is not allowed to specify a namespace: '%s'",
                                                                OPERATION_RAISE_ERROR_IDENTIFIER, s)));
  }

  private static ComponentParameterAst getErrorTypeParam(ComponentAst component) {
    return component.getParameter(DEFAULT_GROUP_NAME, "type");
  }
}
