/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.util.ClassUtils.isInstance;
import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

import java.io.InputStream;
import java.util.Iterator;

import javax.inject.Inject;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;

/**
 * A {@link ValueResolver} implementation and extension of {@link TypeSafeExpressionValueResolver } which evaluates expressions
 * and tries to ensure that the output is always of a certain type.
 * <p>
 * This {@link ValueResolver} will return the {@link TypedValue} of the expression evaluation result.
 *
 * @param <T>
 * @since 4.0
 */
public class ExpressionTypedValueValueResolver<T> extends ExpressionValueResolver<TypedValue<T>> implements Initialisable {

  private final Class<T> expectedClass;
  private final boolean content;
  private TypeSafeTransformer typeSafeTransformer;

  @Inject
  private TransformationService transformationService;

  public ExpressionTypedValueValueResolver(String expression, Class<T> expectedClass) {
    this(expression, expectedClass, false);
  }

  public ExpressionTypedValueValueResolver(String expression, Class<T> expectedClass, boolean content) {
    super(expression, DataType.fromType(expectedClass));
    this.expectedClass = expectedClass;
    this.content = content;
  }

  @Override
  public TypedValue<T> resolve(ValueResolvingContext context) throws MuleException {
    return resolve(context, NO_OP_INSTANCE);
  }

  @Override
  public TypedValue<T> resolve(ValueResolvingContext context, CursorComponentDecoratorFactory factory) throws MuleException {
    TypedValue<T> typedValue = resolveTypedValue(context);
    if (!isInstance(expectedClass, typedValue.getValue())) {
      DataType expectedDataType =
          DataType.builder()
              .type(expectedClass)
              .mediaType(typedValue.getDataType().getMediaType())
              .build();
      typedValue =
          new TypedValue<>(typeSafeTransformer.<T>transform(typedValue.getValue(), typedValue.getDataType(), expectedDataType),
                           expectedDataType);
    }
    return new TypedValue<>(decorateValue(typedValue.getValue(), context.getEvent().getCorrelationId(), factory),
                            typedValue.getDataType());
  }

  private T decorateValue(Object decorated, String correlationId, CursorComponentDecoratorFactory factory) {
    Object decoratedOutput;

    if (decorated instanceof InputStream) {
      decoratedOutput = factory.decorateInput((InputStream) decorated, correlationId);
    } else if (decorated instanceof Iterator) {
      decoratedOutput = factory.decorateInput((Iterator) decorated, correlationId);
    } else {
      decoratedOutput = decorated;
    }

    return (T) decoratedOutput;
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    this.typeSafeTransformer = new TypeSafeTransformer(transformationService);
  }

  @Override
  public boolean isContent() {
    return content;
  }
}
