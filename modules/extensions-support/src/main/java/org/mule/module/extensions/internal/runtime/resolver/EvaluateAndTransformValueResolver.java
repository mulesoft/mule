/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transformer.MessageTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.extensions.introspection.DataType;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link ValueResolver} which evaluates a MEL expressions and tries
 * to ensure that the output is always of a certain type.
 * <p/>
 * If the MEL expression does not return a value of that type, then it
 * tries to locate a {@link Transformer} which can do the transformation
 * from the obtained type to the expected one.
 * <p/>
 * It resolves the expressions by making use of the {@link AttributeEvaluator}
 * so that it's compatible with simple expressions and templates alike
 *
 * @param <T>
 * @since 3.7.0
 */
public class EvaluateAndTransformValueResolver<T> implements ValueResolver<T>, MuleContextAware
{

    private final DataType expectedType;
    private AttributeEvaluator evaluator;
    private MuleContext muleContext;

    public EvaluateAndTransformValueResolver(String expression, DataType expectedType)
    {
        checkArgument(!StringUtils.isBlank(expression), "Expression cannot be blank or null");
        checkArgument(expectedType != null, "expected type cannot be null");

        this.expectedType = expectedType;
        evaluator = new AttributeEvaluator(expression);
    }

    @Override
    public T resolve(MuleEvent event) throws MuleException
    {
        T evaluated = (T) evaluator.resolveValue(event);
        return evaluated != null ? transform(evaluated, event) : null;
    }

    private T transform(T object, MuleEvent event) throws MuleException
    {
        if (expectedType.getRawType().isInstance(object))
        {
            return object;
        }

        Type expectedClass = expectedType.getRawType();
        if (expectedClass instanceof ParameterizedType)
        {
            expectedClass = ((ParameterizedType) expectedClass).getRawType();
        }

        org.mule.api.transformer.DataType sourceDataType = DataTypeFactory.create(object.getClass());
        org.mule.api.transformer.DataType targetDataType = DataTypeFactory.create((Class) expectedClass);

        Transformer transformer;
        try
        {
            transformer = muleContext.getRegistry().lookupTransformer(sourceDataType, targetDataType);
        }
        catch (TransformerException e)
        {
            // no transformer found. Return the object we have and let's hope for the best
            return object;
        }

        if (transformer != null)
        {
            if (transformer instanceof MessageTransformer)
            {
                return (T) ((MessageTransformer) transformer).transform(object, event);
            }
            else
            {
                return (T) transformer.transform(object);
            }
        }

        return object;
    }

    /**
     * @return {@value true}
     */
    @Override
    public boolean isDynamic()
    {
        return true;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
        evaluator.initialize(muleContext.getExpressionManager());
    }
}
