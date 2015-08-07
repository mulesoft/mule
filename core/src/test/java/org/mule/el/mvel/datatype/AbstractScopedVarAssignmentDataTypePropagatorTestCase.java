/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.transport.PropertyScope;

import org.junit.Test;

public abstract class AbstractScopedVarAssignmentDataTypePropagatorTestCase extends AbstractVarAssignmentDataTypePropagatorTestCase
{

    protected final PropertyScope scope;
    protected final String variableName;

    public AbstractScopedVarAssignmentDataTypePropagatorTestCase(EnricherDataTypePropagator dataTypePropagator, PropertyScope scope, String variableName)
    {
        super(dataTypePropagator);
        this.scope = scope;
        this.variableName = variableName;
    }

    @Test
    public void propagatesFlowVarDataTypeUsingMapSyntax() throws Exception
    {
        doAssignmentDataTypePropagationTest(scope, createAssignmentExpression("['" + PROPERTY_NAME + "']"));
    }

    @Test
    public void propagatesFlowVarDataTypeUsingDotSyntax() throws Exception
    {
        doAssignmentDataTypePropagationTest(scope, createAssignmentExpression("." + PROPERTY_NAME + ""));
    }

    @Test
    public void propagatesFlowVarDataTypeUsingEscapedDotSyntax() throws Exception
    {
        doAssignmentDataTypePropagationTest(scope, createAssignmentExpression(".'" + PROPERTY_NAME + "'"));
    }

    private String createAssignmentExpression(String accessorExpression)
    {
        return variableName + accessorExpression + " = 'unused'";
    }
}
