/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel.datatype;

import org.mule.api.MuleMessage;
import org.mule.mvel2.ast.ASTNode;
import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.mvel2.util.ASTIterator;
import org.mule.mvel2.util.ASTLinkedList;
import org.mule.transformer.types.TypedValue;

/**
 * Base class {@link EnricherDataTypePropagator}
 */
public abstract class AbstractEnricherDataTypePropagator implements EnricherDataTypePropagator
{

    @Override
    public boolean propagate(MuleMessage message, TypedValue typedValue, CompiledExpression compiledExpression)
    {
        ASTIterator iterator = new ASTLinkedList(compiledExpression.getFirstNode());

        if (iterator.hasMoreNodes())
        {
            ASTNode node = iterator.nextNode();

            return doPropagate(message, typedValue, node);
        }

        return false;
    }

    protected abstract boolean doPropagate(MuleMessage message, TypedValue typedValue, ASTNode node);
}
