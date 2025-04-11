/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsComponentId;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

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
 * Every handler (except for the last one) within an 'error-handler' must specify a 'when' or 'type' attribute.
 *
 */
// TODO split the ref validation part?
public class ErrorHandlerOnErrorHasTypeOrWhen implements Validation {

  private static final String ON_ERROR = "on-error";
  private static final String WHEN_CHOICE_ES_ATTRIBUTE = "when";
  private static final String TYPE_ES_ATTRIBUTE = "type";
  private static final String ERROR_HANDLER = "error-handler";
  private static final String REFERENCE_ATTRIBUTE = "ref";

  private static final ComponentIdentifier ERROR_HANDLER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ERROR_HANDLER).build();
  private static final ComponentIdentifier ON_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR).build();

  @Override
  public String getName() {
    return "'error-handler': 'on-error' has 'type' or 'when'";
  }

  @Override
  public String getDescription() {
    return "Every handler (except for the last one) within an 'error-handler' must specify a 'when' or 'type' attribute.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return h -> {
      if (h.size() < 2) {
        return false;
      }

      final ComponentAst errorHandler = h.get(h.size() - 2);

      return equalsIdentifier(ERROR_HANDLER_IDENTIFIER)
          .test(errorHandler)
          // last error handler should be the catch all, so that one is not validated
          && currentElemement(ehc -> errorHandler.directChildrenStream().limit(errorHandler.directChildrenStream().count() - 1)
              .anyMatch(comp -> comp.equals(ehc)))
              .test(h);
    };
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst onErrorModel, ArtifactAst artifact) {
    if (ON_ERROR_IDENTIFIER.equals(onErrorModel.getIdentifier())) {
      final ComponentParameterAst errorRefParam = onErrorModel.getParameter(DEFAULT_GROUP_NAME, REFERENCE_ATTRIBUTE);
      final Optional<String> refAttr = errorRefParam.getValue().getValue();
      final Optional<ComponentAst> referenced = refAttr
          .flatMap(sharedOnErrorName -> artifact.topLevelComponentsStream()
              .filter(equalsComponentId(sharedOnErrorName))
              .findAny());

      if (refAttr.isPresent() && !referenced.isPresent()) {
        return of(create(onErrorModel, errorRefParam, this,
                         format("Could not find 'on-error' reference named '%s'", refAttr.get())));
      }

      onErrorModel = referenced.get();
    }

    if (!isParameterPresent(onErrorModel, WHEN_CHOICE_ES_ATTRIBUTE) && !isParameterPresent(onErrorModel, TYPE_ES_ATTRIBUTE)) {
      return of(create(onErrorModel, this,
                       "Every handler (except for the last one) within an 'error-handler' must specify a 'when' or 'type' attribute."));
    }

    return empty();
  }

  boolean isParameterPresent(ComponentAst componentAst, String name) {
    ComponentParameterAst parameterAst = componentAst.getParameter(DEFAULT_GROUP_NAME, name);
    if (parameterAst == null) {
      return false;
    }

    return parameterAst.getValue().getValue().isPresent();
  }

}
