/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.transformer.DataType;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.TypedValue;

/**
 * Base class for {@link ExpressionEvaluator} which provides an implementation of
 * #evaluateTyped (String, MuleMessage) returning a default data type.
 */
public abstract class AbstractExpressionEvaluator implements ExpressionEvaluator
{

    @Override
    public TypedValue evaluateTyped(String expression, MuleMessage message)
    {
        Object value = evaluate(expression, message);
        final DataType<?> dataType = DataTypeFactory.create(value == null ? Object.class : value.getClass(), null);

        return new TypedValue(value, dataType);
    }
}
