/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.el;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.el.context.MessageContext;

class XPathFunction implements ExpressionLanguageFunction
{
    protected MuleContext muleContext;

    public XPathFunction(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object call(Object[] params, ExpressionLanguageContext context)
    {
        try
        {
            MessageContext ctxMessage = (MessageContext) context.getVariable("message");
            MuleMessage message = new DefaultMuleMessage(ctxMessage.getPayload(), muleContext);
            String evaluator = "xpath-branch:";
            if (params.length != 1)
            {
                evaluator = "xpath-node:";
                message = new DefaultMuleMessage(params[1], muleContext);
            }
            Object result = muleContext.getExpressionManager().evaluate(evaluator + params[0], message);
            ctxMessage.setPayload(message.getPayload());
            return result;

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
