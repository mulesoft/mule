/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.expression.ExpressionConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a single expression and adds the results of the expression as individual message parts.
 */
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
    protected List splitMessage(MuleMessage message)
    {
        List results = new ArrayList(4);
        Object result = muleContext.getExpressionManager().evaluate(config.getFullExpression(expressionManager), message);
        if (result instanceof List)
        {
            results.addAll((List)result);
        }
        else
        {
            results.add(result);
            logger.debug("Splitter only returned a single result. If this is not expected, please check your split expression");
        }
        return results;
    }
}
