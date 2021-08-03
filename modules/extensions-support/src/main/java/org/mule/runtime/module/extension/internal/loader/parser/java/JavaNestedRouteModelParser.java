/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Optional.empty;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getOperationFieldParameterGroupParsers;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class JavaNestedRouteModelParser implements NestedRouteModelParser {

  private final ExtensionParameter route;
  private final ClassTypeLoader typeLoader;
  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();

  public JavaNestedRouteModelParser(ExtensionParameter route, ClassTypeLoader typeLoader) {
    this.route = route;
    this.typeLoader = typeLoader;

    route.getType().getDeclaringClass()
        .ifPresent(clazz -> additionalModelProperties.add(new ImplementingTypeModelProperty(clazz)));
  }

  @Override
  public String getName() {
    return route.getAlias();
  }

  @Override
  public String getDescription() {
    return route.getDescription();
  }

  @Override
  public int getMinOccurs() {
    return route.isRequired() ? 1 : 0;
  }

  @Override
  public Optional<Integer> getMaxOccurs() {
    return empty();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    final List<FieldElement> parameters = route.getType().getAnnotatedFields(
        Parameter.class,
        org.mule.sdk.api.annotation.param.Parameter.class);

    return getOperationFieldParameterGroupParsers(parameters, typeLoader);
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }
}
