/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.extension.api.util.NameUtils.pluralize;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Component has a child element which is used for the same purpose as an also present parameter.
 */
public class ParameterAndChildForSameAttributeNotDefinedTogether implements Validation {

  @Override
  public String getName() {
    return "Parameter and child for same attribute are not defined together";
  }

  @Override
  public String getDescription() {
    return "Component has a child element which is used for the same purpose as an also present parameter.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(component -> component.directChildrenStream().count() > 0);
  }

  @Override
  public Optional<String> validate(ComponentAst component, ArtifactAst artifact) {
    return component.directChildrenStream()
        .filter(child -> {
          final String paramName = toCamelCase(child.getIdentifier().getName(), "-");
          final String singularParamName = pluralize(paramName);

          return component.getRawParameterValue(paramName).isPresent()
              || component.getRawParameterValue(singularParamName).isPresent();
        })
        .findFirst()
        .map(child -> {
          final String paramName = toCamelCase(child.getIdentifier().getName(), "-");
          final String singularParamName = pluralize(paramName);

          return format("Component '%s' has a child element '%s' which is used for the same purpose of the configuration parameter '%s'. "
              + "Only one must be used.", component.getIdentifier(),
                        child.getIdentifier(),
                        singularParamName);
        });
  }

}
