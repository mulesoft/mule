/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.el.VariableAssignmentCallback;
import org.mule.api.transport.PropertyScope;
import org.mule.el.context.MessageContext;
import org.mule.el.context.MessagePropertyMapContext;

import org.mvel2.ParserContext;

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
            addVariable("payload", message.getPayload(), new VariableAssignmentCallback()
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
