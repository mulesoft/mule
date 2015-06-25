/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.ast.AssignmentNode;
import org.mule.transformer.types.TypedValue;

/**
 * Propagates data type for inlined flow and session vars used for enrichment target
 */
public class PropertyEnricherDataTypePropagator extends AbstractEnricherDataTypePropagator
{

    @Override
    protected boolean doPropagate(MuleMessage message, TypedValue typedValue, ASTNode node)
    {
        if (node instanceof AssignmentNode)
        {
            PropertyScope scope = null;
            String assignmentVar = ((AssignmentNode) node).getAssignmentVar();

            if (message.getPropertyNames(PropertyScope.INVOCATION).contains(assignmentVar))
            {
                scope = PropertyScope.INVOCATION;
            }
            else if (message.getPropertyNames(PropertyScope.SESSION).contains(assignmentVar))
            {
                scope = PropertyScope.SESSION;
            }

            if (scope != null)
            {
                message.setProperty(assignmentVar, typedValue.getValue(), scope, typedValue.getDataType());
                return true;
            }
        }

        return false;
    }
}
