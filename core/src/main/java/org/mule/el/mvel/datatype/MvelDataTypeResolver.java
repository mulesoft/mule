/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.transformer.types.DataTypeFactory;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Resolves expression's data type using {@link ExpressionDataTypeResolver}
 */
public class MvelDataTypeResolver
{


    private final List<ExpressionDataTypeResolver> resolvers;

    public MvelDataTypeResolver()
    {
        this(getDefaultDataTypeResolvers());
    }

    public MvelDataTypeResolver(List<ExpressionDataTypeResolver> resolvers)
    {
        this.resolvers = new LinkedList<>(resolvers);
    }

    private static List<ExpressionDataTypeResolver> getDefaultDataTypeResolvers()
    {
        List<ExpressionDataTypeResolver> resolvers;
        resolvers = new LinkedList<>();
        resolvers.add(new PayloadExpressionDataTypeResolver());
        resolvers.add(new PropertyExpressionDataTypeResolver());
        resolvers.add(new FlowVarExpressionDataTypeResolver());
        resolvers.add(new SessionVarExpressionDataTypeResolver());

        return resolvers;
    }

    /**
     * Resolves expression data type
     *
     * @param value result of evaluating the expression on the given message
     * @param message message under evaluation
     * @param serializedExpression expression
     * @return the data type corresponding to the given expression if there is an expression
     *         resolver able to resolved it. A default data type for the value if the expression
     *         was not resolved.
     */
    public DataType resolve(Object value, MuleMessage message, Serializable serializedExpression)
    {
        DataType result = null;

        if (serializedExpression instanceof CompiledExpression)
        {
            CompiledExpression compiledExpression = (CompiledExpression) serializedExpression;

            for (ExpressionDataTypeResolver resolver : resolvers)
            {
                result = resolver.resolve(message, compiledExpression);

                if (result != null)
                {
                    break;
                }
            }
        }

        if (result == null)
        {
            result =  DataTypeFactory.create(value == null ? Object.class : value.getClass(), null);
        }

        return result;
    }
}
