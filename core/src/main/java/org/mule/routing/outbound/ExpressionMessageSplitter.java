/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.expression.ExpressionConfig;
import org.mule.util.expression.ExpressionEvaluatorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a single expression and adds the results of the expression as individual message parts.
 * If you require to execute different expressions for each message part you can use the {@link org.mule.routing.outbound.MultiExpressionMessageSplitter}.
 *
 * @see org.mule.routing.outbound.MultiExpressionMessageSplitter
 */
public class ExpressionMessageSplitter extends AbstractRoundRobinMessageSplitter
{
    protected ExpressionConfig config = new ExpressionConfig();

    public String getCustomEvaluator()
    {
        return config.getCustomEvaluator();
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        config.setCustomEvaluator(customEvaluator);
    }

    public String getEvaluator()
    {
        return config.getEvaluator();
    }

    public void setEvaluator(String evaluator)
    {
        config.setEvaluator(evaluator);
    }

    public String getExpression()
    {
        return config.getExpression();
    }

    public void setExpression(String expression)
    {
        this.config.setExpression(expression);
    }

    //@Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        config.validate();
    }

    //@Override
    protected List splitMessage(MuleMessage message)
    {
        List results = new ArrayList(4);
        Object result = ExpressionEvaluatorManager.evaluate(config.getFullExpression(), message);
        if(result instanceof List)
        {
            results.addAll((List)result);
        }
        else
        {
            results.add(result);
            logger.warn("Splitter only returned a single result. If this is not expected, please check your split expression");
        }
        return results;
    }
}