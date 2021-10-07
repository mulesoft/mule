/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
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

public class ParseTemplateResourceExist implements Validation {

  private static final String PARSE_TEMPLATE_ELEMENT_NAME = "parse-template";

  private static final String LOCATION_PARAM = "location";

  private static final ComponentIdentifier PARSE_TEMPLATE_IDENTIFIER = ComponentIdentifier.builder()
      .namespace(CORE_PREFIX)
      .namespaceUri(CORE_NAMESPACE)
      .name(PARSE_TEMPLATE_ELEMENT_NAME)
      .build();

  private final ClassLoader artifactRegionClassLoader;

  public ParseTemplateResourceExist(ClassLoader artifactRegionClassLoader) {
    this.artifactRegionClassLoader = artifactRegionClassLoader;
  }

  @Override
  public String getName() {
    return "'parse-template' resources exist";
  }

  @Override
  public String getDescription() {
    return "Template file referenced in 'parse-template' exists and is accessible.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return currentElemement(comp -> comp.getIdentifier().equals(PARSE_TEMPLATE_IDENTIFIER)
        && comp.getParameter(DEFAULT_GROUP_NAME, LOCATION_PARAM).getValue().getRight() != null);
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    ComponentParameterAst locationParam = component.getParameter(DEFAULT_GROUP_NAME, LOCATION_PARAM);
    String location = (String) locationParam.getValue().getRight();

    if (artifactRegionClassLoader.getResource(location) == null) {
      return of(create(component, locationParam, this, "Template location: '" + location + "' not found"));
    }

    return empty();
  }

}
