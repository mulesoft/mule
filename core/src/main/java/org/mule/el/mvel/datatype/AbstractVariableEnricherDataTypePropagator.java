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
import org.mule.mvel2.ast.Assignment;
import org.mule.mvel2.ast.AssignmentNode;
import org.mule.mvel2.ast.DeepAssignmentNode;
import org.mule.transformer.types.TypedValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class {@link EnricherDataTypePropagator} that propagate data type to message properties.
 */
public class AbstractVariableEnricherDataTypePropagator extends AbstractEnricherDataTypePropagator
{

    private static final Pattern mapSyntaxAccessorMatcher = Pattern.compile("\\['(.*)'\\]");
    private static final Pattern escapedDotSyntaxAccessorMatcher = Pattern.compile(".'(.*)'");
    private static final Pattern dotSyntaxAccessorMatcher = Pattern.compile(".(.*)");

    private final String propertyName;
    private final PropertyScope scope;

    public AbstractVariableEnricherDataTypePropagator(String propertyName, PropertyScope scope)
    {
        this.propertyName = propertyName;
        this.scope = scope;
    }

    @Override
    protected boolean doPropagate(MuleMessage message, TypedValue typedValue, ASTNode node)
    {
        if (isAssignmentNode(node))
        {
            Assignment assignmentNode = (Assignment) node;

            String propertyName = getPropertyName(assignmentNode.getAssignmentVar());

            if (propertyName != null && message.getPropertyNames(scope).contains(propertyName))
            {
                message.setProperty(propertyName, typedValue.getValue(), scope, typedValue.getDataType());

                return true;
            }
        }

        return false;
    }

    private String getPropertyName(String assignmentVar)
    {
        String subExpression = assignmentVar.substring(propertyName.length()).trim();

        Matcher matcher = mapSyntaxAccessorMatcher.matcher(subExpression);
        if (!matcher.matches())
        {
            matcher = escapedDotSyntaxAccessorMatcher.matcher(subExpression);
            if (!matcher.matches())
            {
                matcher = dotSyntaxAccessorMatcher.matcher(subExpression);
                if (!matcher.matches())
                {
                    return null;
                }
            }
        }

        return matcher.group(1);
    }

    private boolean isAssignmentNode(ASTNode node)
    {
        return node instanceof AssignmentNode && ((AssignmentNode) node).getAssignmentVar().startsWith(propertyName)
               || node instanceof DeepAssignmentNode && ((DeepAssignmentNode) node).getAssignmentVar().startsWith(propertyName);
    }
}
