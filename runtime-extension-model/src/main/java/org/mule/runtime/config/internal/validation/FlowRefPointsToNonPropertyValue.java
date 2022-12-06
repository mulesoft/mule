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
import static org.mule.runtime.ast.api.util.MuleAstUtils.hasPropertyPlaceholder;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.apache.commons.lang3.StringUtils.isEmpty;

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
public class FlowRefPointsToNonPropertyValue implements Validation {

  private static final String FLOW_REF_ELEMENT = "flow-ref";

  public static final ComponentIdentifier FLOW_REF_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FLOW_REF_ELEMENT).build();

  private final boolean enabled;

  public FlowRefPointsToNonPropertyValue(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getName() {
    return "'flow-ref's point to fixed flows";
  }

  @Override
  public String getDescription() {
    return "'flow-ref's point to fixed flows.";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    if (enabled) {
      return currentElemement(equalsIdentifier(FLOW_REF_IDENTIFIER))
          .and(currentElemement(component -> !isEmpty(component.getParameter(DEFAULT_GROUP_NAME, "name").getRawValue())));
    } else {
      return c -> false;
    }
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final ComponentParameterAst param = component.getParameter(DEFAULT_GROUP_NAME, "name");

    String nameAttributeRawValue = param.getRawValue();
    if (hasPropertyPlaceholder(nameAttributeRawValue)) {
      return of(create(component, param, this,
                       "'flow-ref' is pointing to '" + nameAttributeRawValue
                           + "' which is resolved with a property and may cause the artifact to have a different structure on different environments."));
    } else {
      return empty();
    }
  }

}
