/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsIdentifier;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.exception.PropertyNotFoundException;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ParseTemplateResourceNotPropertyValue implements Validation {

  private static final String PARSE_TEMPLATE_ELEMENT_NAME = "parse-template";

  private static final String LOCATION_PARAM = "location";

  private static final ComponentIdentifier PARSE_TEMPLATE_IDENTIFIER = ComponentIdentifier.builder()
      .namespace(CORE_PREFIX)
      .namespaceUri(CORE_NAMESPACE)
      .name(PARSE_TEMPLATE_ELEMENT_NAME)
      .build();

  private final boolean enabled;

  public ParseTemplateResourceNotPropertyValue(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getName() {
    return "'parse-template' resources are fixed";
  }

  @Override
  public String getDescription() {
    return "Template file referenced in 'parse-template' is fixed.";
  }

  @Override
  public Level getLevel() {
    return WARN;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(equalsIdentifier(PARSE_TEMPLATE_IDENTIFIER))
        .and(currentElemement(component -> {
          try {
            return component.getParameter(DEFAULT_GROUP_NAME, LOCATION_PARAM).getValue().getRight() != null;
          } catch (PropertyNotFoundException pnfe) {
            if (enabled) {
              return false;
            } else {
              throw pnfe;
            }
          }
        }));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    ComponentParameterAst locationParam = component.getParameter(DEFAULT_GROUP_NAME, LOCATION_PARAM);
    String locationAttributeRawValue = locationParam.getRawValue();

    if (locationAttributeRawValue.contains("${")) {
      return of(create(component, locationParam, this,
                       "'" + PARSE_TEMPLATE_ELEMENT_NAME + "' is pointing to '" + locationAttributeRawValue
                           + "' which is resolved with a property and may cause the artifact to have a different structure on different environments."));
    } else {
      return empty();
    }
  }

}
