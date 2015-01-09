/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.el.VariableAssignmentCallback;
import org.mule.api.transport.PropertyScope;
import org.mule.el.context.MessageContext;
import org.mule.el.context.MessagePropertyMapContext;

import org.mule.mvel2.ParserContext;

class MessageVariableResolverFactory extends MVELExpressionLanguageContext
{

    private static final long serialVersionUID = -6819292692339684915L;

    public MessageVariableResolverFactory(final ParserContext parserContext,
                                          final MuleContext muleContext,
                                          final MuleMessage message)
    {
        super(parserContext, muleContext);

        if (message != null)
        {
            addFinalVariable("message", new MessageContext(message));

            // We need payload top-level for compatibility with payload expression evaluator without ':'
            addVariable("payload", new MessageContext(message).getPayload(), new VariableAssignmentCallback()
            {
                @Override
                public void assignValue(String name, Object value, Object newValue)
                {
                    message.setPayload(newValue);
                }
            });

            // Only add exception is present
            if (message.getExceptionPayload() != null)
            {
                addFinalVariable("exception", message.getExceptionPayload().getException());
            }
            else
            {
                addFinalVariable("exception", null);
            }

            addFinalVariable("flowVars", new MessagePropertyMapContext(message, PropertyScope.INVOCATION));
            addFinalVariable("sessionVars", new MessagePropertyMapContext(message, PropertyScope.SESSION));
        }
    }
}
