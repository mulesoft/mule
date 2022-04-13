/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.util.ClassUtils.isInstance;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;

import javax.inject.Inject;

/**
 * Value resolver that relies on a expression language transformation to convert the delegate resolver into the expected type.
 * 
 * @since 4.5
 */
public class ExpressionLanguageTransformationValueResolver implements ValueResolver {

  private static final String PAYLOAD_IDENTIFIER = "payload";
  private static final String PAYLOAD_EXPRESSION = "#[" + PAYLOAD_IDENTIFIER + "]";

  private Class expectedType;
  private ValueResolver valueResolverDelegate;

  @Inject
  private ExpressionLanguage expressionLanguage;

  @Inject
  private MuleContext muleContext;

  public ExpressionLanguageTransformationValueResolver(ValueResolver valueResolverDelegate, Class expectedType) {
    this.expectedType = expectedType;
    this.expectedType = this.expectedType == null ? Object.class : this.expectedType;
    this.valueResolverDelegate = valueResolverDelegate;
  }

  @Override
  public Object resolve(ValueResolvingContext context) throws MuleException {
    Object resolvedValue = valueResolverDelegate.resolve(context);
    if (isInstance(expectedType, resolvedValue)) {
      return resolvedValue;
    } else {
      return expressionLanguage
          .evaluate(PAYLOAD_EXPRESSION, DataType.fromType(expectedType),
                    BindingContext.builder()
                        .addBinding(PAYLOAD_IDENTIFIER, new TypedValue(resolvedValue, DataType.fromObject(resolvedValue)))
                        .build())
          .getValue();
    }
  }

  @Override
  public boolean isDynamic() {
    return valueResolverDelegate.isDynamic();
  }

}
