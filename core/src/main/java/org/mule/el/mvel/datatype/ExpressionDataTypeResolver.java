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

/**
 * Resolves {@link DataType} from a compiled MEL expression
 */
public interface ExpressionDataTypeResolver
{

    /**
     * Resolves the data type form a given expression
     *
     * @param message mule message being executed
     * @param compiledExpression compiled MEL expression
     * @return a non null data type corresponding to the expression if the resolver
     *         is able to resolve it, null otherwise
     */
    DataType resolve(MuleMessage message, CompiledExpression compiledExpression);
}
