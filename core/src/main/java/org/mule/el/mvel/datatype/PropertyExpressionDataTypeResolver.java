/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import static org.mule.api.transport.PropertyScope.INVOCATION;
import static org.mule.api.transport.PropertyScope.SESSION;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.mvel2.ast.ASTNode;

/**
 * Resolves data type for expressions representing a reference to a invocation
 * or session variable.
 */
public class PropertyExpressionDataTypeResolver extends AbstractExpressionDataTypeResolver
{

    @Override
    protected DataType getDataType(MuleMessage message, ASTNode node)
    {
        if (node.isIdentifier() && message.getPropertyNames(INVOCATION).contains(node.getName()))
        {
            return message.getPropertyDataType(node.getName(), INVOCATION);
        }
        else if (node.isIdentifier() && message.getPropertyNames(SESSION).contains(node.getName()))
        {
            return message.getPropertyDataType(node.getName(), SESSION);
        }
        else
        {
            return null;
        }
    }
}
