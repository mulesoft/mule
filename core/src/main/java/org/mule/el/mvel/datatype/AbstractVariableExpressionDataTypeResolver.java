/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.Accessor;
import org.mule.mvel2.compiler.AccessorNode;
import org.mule.mvel2.compiler.ExecutableLiteral;
import org.mule.mvel2.optimizers.impl.refl.nodes.MapAccessor;
import org.mule.mvel2.optimizers.impl.refl.nodes.MapAccessorNest;
import org.mule.mvel2.optimizers.impl.refl.nodes.VariableAccessor;

/**
 * Base class for extracting data type from map variables
 */
public abstract class AbstractVariableExpressionDataTypeResolver extends AbstractExpressionDataTypeResolver
{

    private final String propertyName;
    private final PropertyScope scope;

    public AbstractVariableExpressionDataTypeResolver(String propertyName, PropertyScope scope)
    {
        this.propertyName = propertyName;
        this.scope = scope;
    }

    @Override
    protected DataType getDataType(MuleMessage message, ASTNode node)
    {
        final Accessor accessor = node.getAccessor();

        if (accessor instanceof VariableAccessor)
        {
            VariableAccessor variableAccessor = (VariableAccessor) accessor;
            if (variableAccessor.getProperty().equals(propertyName))
            {
                final AccessorNode nextNode = variableAccessor.getNextNode();
                String propertyName = null;
                if (nextNode instanceof MapAccessorNest)
                {
                    final MapAccessorNest mapAccesorNest = (MapAccessorNest) nextNode;
                    if (mapAccesorNest.getProperty().isLiteralOnly())
                    {
                        propertyName = (String) ((ExecutableLiteral) mapAccesorNest.getProperty()).getLiteral();
                    }
                }
                else if (nextNode instanceof MapAccessor)
                {
                    propertyName = (String) ((MapAccessor) nextNode).getProperty();
                }

                if (propertyName != null)
                {
                    return message.getPropertyDataType(propertyName, scope);
                }
            }
        }

        return null;
    }
}
