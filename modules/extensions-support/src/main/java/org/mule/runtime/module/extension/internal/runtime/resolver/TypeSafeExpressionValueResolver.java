/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.ClassUtils.isInstance;
import org.apache.commons.lang.StringUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.AttributeEvaluator;

import java.util.function.BiConsumer;

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

  final AttributeEvaluator evaluator;
  private final Class<?> expectedClass;
  private final MuleContext muleContext;

  private final DataType expectedDataType;
  private boolean evaluatorInitialized = false;
  private BiConsumer<AttributeEvaluator, MuleContext> evaluatorInitialiser = (evaluator, context) -> {
    synchronized (context) {
      if (!evaluatorInitialized) {
        evaluator.initialize(context.getExpressionManager());
        evaluatorInitialiser = (e, c) -> {
        };
        evaluatorInitialized = true;
      }
    }
  };

  public TypeSafeExpressionValueResolver(String expression, Class<?> expectedType, MuleContext muleContext) {
    checkArgument(!StringUtils.isBlank(expression), "Expression cannot be blank or null");
    checkArgument(expectedType != null, "expected type cannot be null");

    this.expectedClass = expectedType;
    this.expectedDataType = DataType.fromType(expectedType);
    this.evaluator = new AttributeEvaluator(expression);
    this.muleContext = muleContext;
  }

  @Override
  public T resolve(Event event) throws MuleException {
    initEvaluator();
    TypedValue typedValue = evaluator.resolveTypedValue(event, Event.builder(event));

    Object value = typedValue.getValue();

    if (isInstance(ValueResolver.class, value)) {
      value = ((ValueResolver) value).resolve(event);
    }

    if (isInstance(expectedClass, value)) {
      return (T) value;
    }
    return value != null ? (T) transform(typedValue, expectedDataType, event) : null;
  }

  public Object transform(TypedValue value, DataType expectedDataType, Event event)
      throws MessagingException, MessageTransformerException, TransformerException {
    Transformer transformer;
    try {
      transformer = muleContext.getRegistry().lookupTransformer(value.getDataType(), expectedDataType);
    } catch (TransformerException e) {
      throw new MessagingException(createStaticMessage(String.format(
                                                                     "Expression '%s' was expected to return a value of type '%s' but a '%s' was found instead "
                                                                         + "and no suitable transformer could be located",
                                                                     evaluator.getRawValue(), expectedClass.getName(),
                                                                     value.getValue().getClass().getName())),
                                   event, e);
    }

    T result;
    if (transformer instanceof MessageTransformer) {
      result = (T) ((MessageTransformer) transformer).transform(value.getValue(), event);
    } else {
      result = (T) transformer.transform(value.getValue());
    }
    return result;
  }

  void initEvaluator() {
    evaluatorInitialiser.accept(evaluator, muleContext);
  }

  /**
   * @return {@code true}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }
}
