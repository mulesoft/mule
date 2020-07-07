/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.context;

import static java.util.stream.Collectors.toMap;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DeclarationValueProviderCacheIdGeneratorContext implements ValueProviderCacheIdGeneratorContext<ParameterValue> {

  private ComponentIdentifier componentIdentifier;
  private ParameterizedModel parameterizedModel;
  private Map<String, ParameterInfo<ParameterValue>> parameters;
  private Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>> configContext;
  private LazyValue<Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>>> connectionContext;

  DeclarationValueProviderCacheIdGeneratorContext(ComponentIdentifier componentIdentifier,
                                                  ParameterizedElementDeclaration elementDeclaration,
                                                  ParameterizedModel model,
                                                  Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>> configContext,
                                                  LazyValue<Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>>> connectionContext) {
    this.componentIdentifier = componentIdentifier;
    this.parameterizedModel = model;
    this.parameters = elementDeclaration
        .getParameterGroups()
        .stream()
        .flatMap(pg -> pg.getParameters().stream())
        .collect(toMap(ElementDeclaration::getName, p -> new DefaultParameterInfo<>(
                                                                                    p.getName(),
                                                                                    p.getValue(),
                                                                                    Objects::hash)));
    this.configContext = configContext;
    this.connectionContext = connectionContext;
  }

  @Override
  public ComponentIdentifier getOwnerId() {
    return componentIdentifier;
  }

  @Override
  public ParameterizedModel getOwnerModel() {
    return parameterizedModel;
  }

  @Override
  public Map<String, ParameterInfo<ParameterValue>> getParameters() {
    return parameters;
  }

  @Override
  public Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>> getConfigContext() {
    return configContext;
  }

  @Override
  public Optional<ValueProviderCacheIdGeneratorContext<ParameterValue>> getConnectionContext() {
    return connectionContext.get();
  }

}
