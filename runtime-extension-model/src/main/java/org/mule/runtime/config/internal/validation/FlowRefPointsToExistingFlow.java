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
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsComponentId;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.core.internal.util.ExpressionUtils.isExpression;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Optional.empty;
import static java.util.Optional.of;

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
 * 'flow-ref's point to existing flows.
 */
public class FlowRefPointsToExistingFlow implements Validation {

  private static final String FLOW_REF_ELEMENT = "flow-ref";

  public static final ComponentIdentifier FLOW_REF_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FLOW_REF_ELEMENT).build();

  @Override
  public String getName() {
    return "'flow-ref's point to existing flows";
  }

  @Override
  public String getDescription() {
    return "'flow-ref's point to existing flows.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(FLOW_REF_IDENTIFIER))
        .and(currentElemement(component -> component.getParameter(DEFAULT_GROUP_NAME, "name").getValue().isRight()));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst param = component.getParameter(DEFAULT_GROUP_NAME, "name");
    return param.getValue()
        .reduce(l -> empty(),
                nameAttribute -> {
                  if (isExpression((String) nameAttribute)) {
                    // According to the extension model, flow-ref cannot be dynamic,
                    // But this check is needed to avoid breaking on legacy cases that use dynamic flow-refs.
                    return empty();
                  }

                  if (artifact.topLevelComponentsStream()
                      .noneMatch(equalsComponentId((String) nameAttribute))) {
                    return of(create(component, param, this,
                                     "'flow-ref' is pointing to '" + nameAttribute + "' which does not exist"));
                  } else {
                    return empty();
                  }
                });
  }

}
