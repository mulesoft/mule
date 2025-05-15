/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.metadata.TypedValue.of;
import static org.mule.runtime.api.metadata.TypedValue.unwrap;
import static org.mule.runtime.core.api.util.ClassUtils.isInstance;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;

/**
 * Value resolver that relies on a expression language transformation to convert the delegate resolver into the expected type.
 *
 * @since 4.5
 */
public class ExpressionLanguageTransformationValueResolver implements ValueResolver {

  private static final String PAYLOAD_IDENTIFIER = "payload";
  private static final String PAYLOAD_EXPRESSION = "#[" + PAYLOAD_IDENTIFIER + "]";

  private final Class expectedType;
  private final ValueResolver valueResolverDelegate;
  private final ExpressionLanguage expressionLanguage;

  public ExpressionLanguageTransformationValueResolver(ValueResolver valueResolverDelegate, Class expectedType,
                                                       ExpressionLanguage expressionLanguage) {
    this.expectedType = expectedType == null ? Object.class : expectedType;
    this.valueResolverDelegate = valueResolverDelegate;
    this.expressionLanguage = expressionLanguage;
  }

  @Override
  public Object resolve(ValueResolvingContext context) throws MuleException {
    Object resolvedValue = valueResolverDelegate.resolve(context);
    if (isInstance(expectedType, unwrap(resolvedValue))) {
      return resolvedValue;
    } else {
      return expressionLanguage
          .evaluate(PAYLOAD_EXPRESSION, fromType(expectedType),
                    BindingContext.builder()
                        .addBinding(PAYLOAD_IDENTIFIER, of(resolvedValue))
                        .build())
          .getValue();
    }
  }

  @Override
  public boolean isDynamic() {
    return valueResolverDelegate.isDynamic();
  }

}
