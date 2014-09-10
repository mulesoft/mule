/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.expression.ExpressionConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a single expression and adds the results of the expression as individual message parts.
 *
 * Deprecated from 3.6.0.  This functionality is specific to Services.
 */
@Deprecated
public class ExpressionMessageSplitter extends AbstractRoundRobinMessageSplitter
{
    protected ExpressionConfig config = new ExpressionConfig();

    public ExpressionMessageSplitter()
    {
        super();
    }

    public ExpressionMessageSplitter(ExpressionConfig config)
    {
        this.config = config;
        setEvaluator(config.getEvaluator());
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
        //Switch to XPath node since we want the Dom nodes not the value of the node
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

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        config.validate(expressionManager);
    }

    @Override
    protected List<Object> splitMessage(MuleMessage message)
    {
        List<Object> results = new ArrayList<Object>(4);
        Object result = muleContext.getExpressionManager().evaluate(config.getFullExpression(expressionManager), message);
        if (result instanceof List)
        {
            results.addAll((List<?>)result);
        }
        else
        {
            results.add(result);
            logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
        }
        return results;
    }
}
