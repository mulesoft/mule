/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.expression.ExpressionConfig;

public class SimpleSplitter extends AbstractSplittingInterceptingMessageProcessor implements Initialisable
{

    protected ExpressionManager expressionManager;
    protected ExpressionConfig config = new ExpressionConfig();

    public SimpleSplitter()
    {
        // Used by spring
    }

    public SimpleSplitter(ExpressionConfig config)
    {
        this.config = config;
        setEvaluator(config.getEvaluator());
    }

    public void initialise() throws InitialisationException
    {
        expressionManager = muleContext.getExpressionManager();
        config.validate(expressionManager);
    }

    protected Object splitMessage(MuleEvent event)
    {
        return event.getMuleContext().getExpressionManager().evaluate(
            config.getFullExpression(expressionManager), event.getMessage());
    }

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
        // Switch to XPath node since we want the Dom nodes not the value of the node
        if (evaluator.equals("xpath"))
        {
            evaluator = "xpath-node";
        }
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

}
