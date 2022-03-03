/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_MEL_AS_DEFAULT;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import javax.inject.Inject;

/**
 * {@link ValueResolver} implementation for {@link ParameterResolver} that are resolved from an expression
 *
 * @since 4.0
 */
public class ExpressionBasedParameterResolverValueResolver<T> implements ExpressionBasedValueResolver<ParameterResolver<T>>,
    Initialisable {

  private final String expression;
  private final DataType expectedDataType;

  @Inject
  private TransformationService transformationService;

  @Inject
  private ExtendedExpressionManager extendedExpressionManager;

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  private Class<T> type;
  private boolean melDefault;
  private boolean melAvailable;
  private TypeSafeExpressionValueResolver<T> delegateResolver;

  public ExpressionBasedParameterResolverValueResolver(String expression, Class<T> type, DataType expectedDataType) {
    this.expression = expression;
    this.type = type;
    this.expectedDataType = expectedDataType;
  }

  @Override
  public void initialise() throws InitialisationException {
    melDefault = valueOf(getProperty(MULE_MEL_AS_DEFAULT, "false"));
    melAvailable = registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED).isPresent();

    delegateResolver = new TypeSafeExpressionValueResolver<>(expression, type, expectedDataType);
    delegateResolver.setExtendedExpressionManager(extendedExpressionManager);
    delegateResolver.setTransformationService(transformationService);
    delegateResolver.setMuleContext(muleContext);
    delegateResolver.setMelDefault(melDefault);
    delegateResolver.setMelAvailable(melAvailable);

    delegateResolver.initialise();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterResolver<T> resolve(ValueResolvingContext context) throws MuleException {
    return new ExpressionBasedParameterResolver<>(expression, delegateResolver, context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  public void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getExpression() {
    return expression;
  }
}
