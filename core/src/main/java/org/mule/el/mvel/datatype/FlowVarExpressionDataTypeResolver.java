/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.transport.PropertyScope;

/**
 * Resolves data type for flow var when flowVars['x'] syntax is used
 */
public class FlowVarExpressionDataTypeResolver extends AbstractVariableExpressionDataTypeResolver
{

    public FlowVarExpressionDataTypeResolver()
    {
        super("flowVars", PropertyScope.INVOCATION);
    }
}
