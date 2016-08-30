/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.ClassUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link ValueResolver} which evaluates a MEL expressions and tries to ensure that the output is always of a certain type.
 * <p>
 * If the MEL expression does not return a value of that type, then it tries to locate a {@link Transformer} which can do the
 * transformation from the obtained type to the expected one.
 * <p>
 * It resolves the expressions by making use of the {@link AttributeEvaluator} so that it's compatible with simple expressions and
 * templates alike
 *
 * @param <T>
 * @since 3.7.0
 */
public class TypeSafeExpressionValueResolver<T> implements ValueResolver<T> {

  private final Class<?> expectedType;
  private final AttributeEvaluator evaluator;
  private final MuleContext muleContext;

  public TypeSafeExpressionValueResolver(String expression, Class<?> expectedType, MuleContext muleContext) {
    checkArgument(!StringUtils.isBlank(expression), "Expression cannot be blank or null");
    checkArgument(expectedType != null, "expected type cannot be null");

    this.expectedType = expectedType;
    evaluator = new AttributeEvaluator(expression);
    evaluator.initialize(muleContext.getExpressionManager());

    this.muleContext = muleContext;
  }

  @Override
  public T resolve(MuleEvent event) throws MuleException {
    T evaluated = (T) evaluator.resolveValue(event);
    return evaluated != null ? transform(evaluated, event) : null;
  }

  private T transform(T object, MuleEvent event) throws MuleException {
    if (ClassUtils.isInstance(expectedType, object)) {
      return object;
    }

    Type expectedClass = expectedType;
    if (expectedClass instanceof ParameterizedType) {
      expectedClass = ((ParameterizedType) expectedClass).getRawType();
    }

    DataType sourceDataType = DataType.fromType(object.getClass());
    DataType targetDataType = DataType.fromType((Class<T>) expectedClass);

    Transformer transformer;
    try {
      transformer = muleContext.getRegistry().lookupTransformer(sourceDataType, targetDataType);
    } catch (TransformerException e) {

      throw new MessagingException(createStaticMessage(String.format(
                                                                     "Expression '%s' was expected to return a value of type '%s' but a '%s' was found instead "
                                                                         + "and no suitable transformer could be located",
                                                                     evaluator.getRawValue(), expectedType.getName(),
                                                                     object.getClass().getName())),
                                   event, e);
    }

    if (transformer instanceof MessageTransformer) {
      return (T) ((MessageTransformer) transformer).transform(object, event);
    } else {
      return (T) transformer.transform(object);
    }
  }

  /**
   * @return {@code true}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }

  private interface EvaluatorDelegate {

    Object resolveValue(MuleEvent event);
  }

}
