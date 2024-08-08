/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forRoute;
import static org.mule.runtime.module.extension.internal.loader.parser.java.route.JavaChainParsingUtils.parseChainExecutionOccurrence;

import static java.util.Optional.empty;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.nested.ChainExecutionOccurrence;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.NestedRouteModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils;
import org.mule.sdk.api.annotation.route.ExecutionOccurrence;
import org.mule.sdk.api.runtime.route.Route;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link NestedRouteModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaNestedRouteModelParser implements NestedRouteModelParser {

  private final ExtensionParameter route;
  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();
  private final boolean sdkApiDefined;

  public JavaNestedRouteModelParser(ExtensionParameter route) {
    this.route = route;

    Class clazz = route.getType().getDeclaringClass().orElse(null);
    if (clazz != null) {
      additionalModelProperties.add(new ImplementingTypeModelProperty(clazz));
      sdkApiDefined = Route.class.isAssignableFrom(clazz);
    } else {
      sdkApiDefined = false;
    }
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

    return getParameterGroupParsers(parameters, forRoute(getName()));
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return JavaExtensionModelParserUtils.getDeprecationModel(route);
  }

  @Override
  public List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory) {
    return JavaStereotypeModelParserUtils.getAllowedStereotypes(route, route.getType(), factory);
  }

  @Override
  public ChainExecutionOccurrence getExecutionOccurrence() {
    return parseChainExecutionOccurrence(route.getValueFromAnnotation(ExecutionOccurrence.class));
  }

  @Override
  public Set<String> getSemanticTerms() {
    return new LinkedHashSet<>();
  }

  @Override
  public boolean isSdkApiDefined() {
    return sdkApiDefined;
  }
}
