/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;

import org.mule.module.db.internal.domain.query.QueryParamValue;

import java.util.LinkedList;
import java.util.List;

/**
 * Resolves query parameters evaluating expression using a given event
 */
public class DynamicParamValueResolver implements ParamValueResolver
{

    private final ExpressionManager expressionManager;

    public DynamicParamValueResolver(ExpressionManager expressionManager)
    {
        this.expressionManager = expressionManager;
    }

    @Override
    public List<QueryParamValue> resolveParams(MuleEvent muleEvent, List<QueryParamValue> templateParams)
    {
        List<QueryParamValue> params = new LinkedList<QueryParamValue>();

        if (templateParams != null)
        {
            for (QueryParamValue templateParam : templateParams)
            {
                if (templateParam != null && templateParam.getValue() instanceof String && expressionManager.isExpression((String) templateParam.getValue()))
                {
                    Object newValue = expressionManager.evaluate((String) templateParam.getValue(), muleEvent);
                    QueryParamValue queryParamValue = new QueryParamValue(templateParam.getName(), newValue);

                    params.add(queryParamValue);
                }
                else
                {
                    params.add(templateParam);
                }
            }
        }

        return params;
    }
}
