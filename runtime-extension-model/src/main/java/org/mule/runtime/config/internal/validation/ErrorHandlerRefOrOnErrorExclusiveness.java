/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

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
 * A reference error-handler cannot have on-errors.
 */
public class ErrorHandlerRefOrOnErrorExclusiveness implements Validation {

  private static final String ERROR_HANDLER = "error-handler";
  private static final String REFERENCE_ATTRIBUTE = "ref";

  private final ComponentIdentifier ERROR_HANDLER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ERROR_HANDLER).build();

  @Override
  public String getName() {
    return "'error-handler': 'ref' or 'on-error' exclusiveness";
  }

  @Override
  public String getDescription() {
    return "A reference error-handler cannot have on-errors.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(ERROR_HANDLER_IDENTIFIER));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst refParam = component.getParameter(DEFAULT_GROUP_NAME, REFERENCE_ATTRIBUTE);
    if (refParam.getValue().getValue().isPresent()
        && component.directChildrenStream().count() > 0) {
      return of(create(component, refParam, this, "A reference 'error-handler' cannot have 'on-error's."));
    } else {
      return empty();
    }
  }

}
