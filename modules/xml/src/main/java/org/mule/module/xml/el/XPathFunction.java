/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.util.OneTimeWarning;
import org.mule.util.Preconditions;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated this function is deprecated and will be removed in Mule 4.0. Use {@link XPath3Function} instead
 */
@Deprecated
public class XPathFunction implements ExpressionLanguageFunction
{

    private static final Logger LOGGER = LoggerFactory.getLogger(XPathFunction.class);
    private static final String BRANCH_EVALUATOR = "xpath-branch:";
    private static final String NODE_EVALUATOR = "xpath-node:";

    private MuleContext muleContext;
    private OneTimeWarning deprecationWarning = new OneTimeWarning(LOGGER, "xpath() MEL function has been deprecated in Mule 3.6.0 and will be removed " +
                                                                           "in 4.0. Please use the new xpath3() function instead");

    public XPathFunction(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object call(Object[] params, ExpressionLanguageContext context)
    {

        deprecationWarning.warn();

        this.validateParams(params);

        final MessageContext ctx = context.getVariable("message");
        final String xpathExpression = this.getXpathExpression(params);
        final boolean hasCustomPayload = this.hasCustomPayload(params);

        MuleMessage muleMessage = context.getVariable(MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE);

        if (hasCustomPayload)
        {
            muleMessage = new DefaultMuleMessage(params[1], muleContext);
        }
        else if (muleMessage == null)
        {
            muleMessage = new DefaultMuleMessage(ctx.getPayload(), muleContext);
        }

        String evaluator = hasCustomPayload ? NODE_EVALUATOR : BRANCH_EVALUATOR;

        try
        {
            Object result = muleContext.getExpressionManager().evaluate(evaluator + xpathExpression, muleMessage);

            if (!hasCustomPayload)
            {
                ctx.setPayload(muleMessage.getPayload());
            }

            return result;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void validateParams(Object[] params)
    {
        Preconditions.checkArgument(params.length > 0 && params.length <= 2, String.format("XPath function accepts up to 2 arguments, but %s were provided instead", params.length));
    }

    private String getXpathExpression(Object[] params)
    {
        String expression = (String) params[0];
        Preconditions.checkArgument(!StringUtils.isBlank(expression), "XPath expression cannot be blank");

        return expression;
    }

    private boolean hasCustomPayload(Object[] params)
    {
        return params.length >= 2;
    }
}
