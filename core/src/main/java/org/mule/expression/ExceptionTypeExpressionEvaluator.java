/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.routing.filters.ExceptionTypeFilter;

public class ExceptionTypeExpressionEvaluator implements ExpressionEvaluator
{
    protected transient Log logger = LogFactory.getLog(ExceptionTypeExpressionEvaluator.class);

    @Override
    public Object evaluate(String expression, MuleMessage message)
    {
        return new ExceptionTypeFilter(expression).accept(message);
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return "exception-type";
    }
}
