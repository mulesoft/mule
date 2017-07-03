/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultTransformationService;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import javax.inject.Inject;

/**
 * {@link ValueResolver} implementation for {@link ParameterResolver} that are resolved from an expression
 *
 * @since 4.0
 */
public class ExpressionBasedParameterResolverValueResolver<T> implements ValueResolver<ParameterResolver<T>> {

  private final String exp;
  private final MetadataType metadataType;

  @Inject
  private DefaultTransformationService transformationService;
  @Inject
  private ExtendedExpressionManager extendedExpressionManager;

  public ExpressionBasedParameterResolverValueResolver(String exp, MetadataType metadataType) {
    this.exp = exp;
    this.metadataType = metadataType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterResolver<T> resolve(ValueResolvingContext context) throws MuleException {
    ExpressionBasedParameterResolver<T> resolver = new ExpressionBasedParameterResolver<>(exp, metadataType, context);
    resolver.setTransformationService(transformationService);
    resolver.setExtendedExpressionManager(extendedExpressionManager);
    resolver.initialise();
    return resolver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }

  public void setTransformationService(DefaultTransformationService transformationService) {
    this.transformationService = transformationService;
  }

  public void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

}
