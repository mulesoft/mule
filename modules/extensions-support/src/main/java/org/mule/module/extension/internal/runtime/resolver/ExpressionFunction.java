/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.api.introspection.DataType;

import java.util.function.Function;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents a function that accepts a {@link MuleEvent}
 * and evaluates a MEL expression that produces a
 * result of the specified type using a {@link TypeSafeExpressionValueResolver}.
 *
 * @since 4.0
 */
final class ExpressionFunction<T> implements Function<MuleEvent, T>
{

    private final String expression;
    private final DataType type;

    ExpressionFunction(String expression, DataType type)
    {
        this.expression = expression;
        this.type = type;
    }

    @Override
    public T apply(MuleEvent event)
    {
        try
        {
            return new TypeSafeExpressionValueResolver<T>(expression, type).resolve(event);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ExpressionFunction
               && ((ExpressionFunction) obj).expression.equals(expression)
               && ((ExpressionFunction) obj).type.equals(type);
    }

}
