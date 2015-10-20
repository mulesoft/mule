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
import org.mule.mvel2.ast.DeepAssignmentNode;
import org.mule.mvel2.compiler.AccessorNode;
import org.mule.mvel2.compiler.CompiledAccExpression;
import org.mule.mvel2.compiler.ExecutableLiteral;
import org.mule.mvel2.optimizers.impl.refl.nodes.MapAccessor;
import org.mule.mvel2.optimizers.impl.refl.nodes.MapAccessorNest;
import org.mule.mvel2.optimizers.impl.refl.nodes.VariableAccessor;
import org.mule.transformer.types.TypedValue;

import java.lang.reflect.Field;

/**
 * Base class {@link EnricherDataTypePropagator} that propagate data type to message properties.
 */
public class AbstractVariableEnricherDataTypePropagator extends AbstractEnricherDataTypePropagator
{

    private final String propertyName;
    private final PropertyScope scope;
    private final Field accExprFieldForMapSyntax;
    private final Field accExprFieldForDotSyntax;

    public AbstractVariableEnricherDataTypePropagator(String propertyName, PropertyScope scope)
    {
        this.propertyName = propertyName;
        this.scope = scope;

        try
        {
            // Needs to use reflection to access parsing information not available on the available interfaces
            accExprFieldForMapSyntax = AssignmentNode.class.getDeclaredField("accExpr");
            accExprFieldForMapSyntax.setAccessible(true);

            accExprFieldForDotSyntax = DeepAssignmentNode.class.getDeclaredField("acc");
            accExprFieldForDotSyntax.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected boolean doPropagate(MuleMessage message, TypedValue typedValue, ASTNode node)
    {
        if (isAssignmentNode(node))
        {
            CompiledAccExpression compiledAccExpression = getCompiledAccExpression(node);

            if (compiledAccExpression.getAccessor() instanceof VariableAccessor)
            {
                VariableAccessor variableAccessor = (VariableAccessor) compiledAccExpression.getAccessor();
                if (variableAccessor.getProperty().equals(propertyName))
                {
                    final AccessorNode nextNode = variableAccessor.getNextNode();

                    if (nextNode.getNextNode() == null)
                    {
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

                        if (propertyName != null && message.getPropertyNames(scope).contains(propertyName))
                        {
                            propertyName = getUnescapedPropertyName(propertyName);
                            message.setProperty(propertyName, typedValue.getValue(), scope, typedValue.getDataType());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private String getUnescapedPropertyName(String propertyName)
    {
        if (propertyName.startsWith("'") && propertyName.endsWith("'"))
        {
            propertyName = propertyName.substring(1, propertyName.length() -1);
        }

        return propertyName;
    }

    private boolean isAssignmentNode(ASTNode node)
    {
        return node instanceof AssignmentNode && ((AssignmentNode) node).getAssignmentVar().startsWith(propertyName)
               || node instanceof DeepAssignmentNode && ((DeepAssignmentNode) node).getAssignmentVar().startsWith(propertyName);
    }

    private CompiledAccExpression getCompiledAccExpression(ASTNode node)
    {
        try
        {
            if (node instanceof AssignmentNode)
            {
                return (CompiledAccExpression) accExprFieldForMapSyntax.get(node);
            }
            else
            {
                return (CompiledAccExpression) accExprFieldForDotSyntax.get(node);
            }
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
