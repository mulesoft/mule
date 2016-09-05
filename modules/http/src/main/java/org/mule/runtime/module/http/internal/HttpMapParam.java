/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;

import java.util.Iterator;
import java.util.Map;


public class HttpMapParam extends HttpParam {

  private String expression;

  public HttpMapParam(HttpParamType type) {
    super(type);
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  @Override
  public void resolve(ParameterMap parameterMap, MuleEvent muleEvent, MuleContext muleContext) {
    Map<Object, Object> paramMap = muleContext.getExpressionLanguage().evaluate(expression, muleEvent, null);
    for (Map.Entry<Object, Object> entry : paramMap.entrySet()) {
      String paramName = entry.getKey().toString();
      Object paramValue = entry.getValue();

      if (paramValue instanceof Iterable) {
        Iterable iterable = (Iterable) paramValue;
        final Iterator iterator = iterable.iterator();
        while (iterator.hasNext()) {
          parameterMap.put(paramName, toStringIfPossible(iterator.next()));
        }
      } else {
        parameterMap.put(paramName, toStringIfPossible(paramValue));
      }

    }
  }

  private String toStringIfPossible(Object paramValue) {
    return paramValue != null ? paramValue.toString() : null;
  }
}
