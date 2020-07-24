/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.context;

import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedIdHelper;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AstValueProviderCacheIdGeneratorContext implements ValueProviderCacheIdGeneratorContext<ComponentParameterAst> {

  private static final LazyValue<Optional<ValueProviderCacheIdGeneratorContext<ComponentParameterAst>>> NULL_LAZY_VALUE =
      new LazyValue<>(Optional::empty);

  private ComponentAst componentAst;
  private ParameterizedModel parameterizedModel;
  private Map<String, ParameterInfo<ComponentParameterAst>> parameters;
  private Optional<ValueProviderCacheIdGeneratorContext<ComponentParameterAst>> configContext;
  private LazyValue<Optional<ValueProviderCacheIdGeneratorContext<ComponentParameterAst>>> connectionContext;

  public AstValueProviderCacheIdGeneratorContext(ComponentAst componentAst) {
    this.componentAst = componentAst;
    this.parameterizedModel = componentAst.getModel(ParameterizedModel.class)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("ValueProviderCacheIdContext has to be created from a ParameterizedModel")));
    this.parameters = componentAst.getParameters()
        .stream()
        .collect(toMap(p -> p.getModel().getName(),
                       p -> new DefaultParameterInfo<>(p.getModel().getName(),
                                                       p,
                                                       ComponentBasedIdHelper::computeHashFor)));
    this.configContext = empty();
    this.connectionContext = NULL_LAZY_VALUE;
  }

  public AstValueProviderCacheIdGeneratorContext(ComponentAst componentAst, ComponentAst configAst) {
    this(componentAst);
    this.configContext = Optional.of(new AstValueProviderCacheIdGeneratorContext(configAst));
    this.connectionContext = new LazyValue<>(
                                             () -> configAst.directChildrenStream()
                                                 .filter(nested -> nested.getModel(ConnectionProviderModel.class).isPresent())
                                                 .findAny()
                                                 .map(
                                                      AstValueProviderCacheIdGeneratorContext::new));
  }

  @Override
  public ComponentIdentifier getOwnerId() {
    return componentAst.getIdentifier();
  }

  @Override
  public ParameterizedModel getOwnerModel() {
    return parameterizedModel;
  }

  @Override
  public Map<String, ParameterInfo<ComponentParameterAst>> getParameters() {
    return this.parameters;
  }

  @Override
  public Optional<ValueProviderCacheIdGeneratorContext<ComponentParameterAst>> getConfigContext() {
    return configContext;
  }

  @Override
  public Optional<ValueProviderCacheIdGeneratorContext<ComponentParameterAst>> getConnectionContext() {
    return connectionContext.get();
  }

}
