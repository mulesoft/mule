/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.param;

import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionLanguage;

import java.util.LinkedList;
import java.util.List;

/**
 * Resolves query parameters evaluating expression using a given event
 */
public class DynamicParamValueResolver implements ParamValueResolver {

  private final ExpressionLanguage expressionLanguage;

  public DynamicParamValueResolver(ExpressionLanguage expressionLanguage) {
    this.expressionLanguage = expressionLanguage;
  }

  @Override
  public List<QueryParamValue> resolveParams(Event muleEvent, List<QueryParamValue> templateParams) {
    List<QueryParamValue> params = new LinkedList<>();

    if (templateParams != null) {
      for (QueryParamValue templateParam : templateParams) {
        if (templateParam != null && templateParam.getValue() instanceof String
            && expressionLanguage.isExpression((String) templateParam.getValue())) {
          Object newValue = expressionLanguage.evaluate((String) templateParam.getValue(), muleEvent, null);
          QueryParamValue queryParamValue = new QueryParamValue(templateParam.getName(), newValue);

          params.add(queryParamValue);
        } else {
          params.add(templateParam);
        }
      }
    }

    return params;
  }
}
