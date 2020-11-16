/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsComponentId;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.FLOW_REF_IDENTIFIER;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 'flow-ref's point to existing flows.
 */
public class FlowRefPointsToExistingFlow implements Validation {

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
        .and(currentElemement(componentModel -> componentModel.getParameter("name").getValue().isRight()));
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    return component.getParameter("name").getValue()
        .reduce(l -> empty(),
                nameAttribute -> {
                  if (((String) nameAttribute).startsWith(DEFAULT_EXPRESSION_PREFIX)
                      && ((String) nameAttribute).endsWith(DEFAULT_EXPRESSION_POSTFIX)) {
                    // According to the extension model, flow-ref cannot be dynamic,
                    // But this check is needed to avoid breaking on legacy cases that use dynamic flow-refs.
                    return empty();
                  }

                  if (artifact.topLevelComponentsStream()
                      .noneMatch(equalsComponentId((String) nameAttribute))) {
                    return of("'flow-ref' is pointing to '" + nameAttribute
                        + "' which does not exist");
                  } else {
                    return empty();
                  }
                });
  }

}
