/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
