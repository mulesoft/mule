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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.RouterResultsHandler;
import org.mule.expression.ExpressionConfig;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.routing.outbound.DefaultRouterResultsHandler;

import java.util.ArrayList;
import java.util.List;

public class SimpleSplitter extends AbstractInterceptingMessageProcessor
    implements MuleContextAware, Initialisable
{

    protected MuleContext muleContext;
    protected ExpressionManager expressionManager;
    protected ExpressionConfig config = new ExpressionConfig();
    protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
    protected CorrelationMode enableCorrelation = CorrelationMode.IF_NOT_SET;

    public SimpleSplitter()
    {
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

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        List results = new ArrayList(4);
        Object result = event.getMuleContext().getExpressionManager().evaluate(
            config.getFullExpression(expressionManager), event.getMessage());
        if (result instanceof List)
        {
            return resultsHandler.aggregateResults(processParts((List) result, event), event, muleContext);
        }
        else
        {
            logger.warn("Splitter only returned a single result. If this is not expected, please check your split expression");
            MuleEvent resultEvent;
            if (result instanceof MuleMessage)
            {
                resultEvent = new DefaultMuleEvent((MuleMessage) result, event);
            }
            else
            {
                resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(result, muleContext), event);
            }
            return processNext(event);
        }
    }

    protected List<MuleEvent> processParts(List parts, MuleEvent event) throws MuleException
    {
        String correlationId = event.getFlowConstruct().getMessageInfoMapping().getCorrelationId(
            event.getMessage());
        List<MuleEvent> resultEvents = new ArrayList<MuleEvent>();
        int correlationSequence = 1;

        for (Object part : parts)
        {
            MuleMessage message;
            if (part instanceof MuleMessage)
            {
                message = (MuleMessage) part;
            }
            else
            {
                message = new DefaultMuleMessage(part, muleContext);
            }

            if (enableCorrelation != CorrelationMode.NEVER)
            {
                boolean correlationSet = message.getCorrelationId() != null;
                if ((!correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
                    || (enableCorrelation == CorrelationMode.ALWAYS))
                {
                    message.setCorrelationId(correlationId);
                }

                // take correlation group size from the message properties, set by concrete
                // message splitter implementations
                message.setCorrelationGroupSize(parts.size());
                message.setCorrelationSequence(correlationSequence++);
            }
            resultEvents.add(processNext(new DefaultMuleEvent(message, event)));
        }
        return resultEvents;
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

    public void setEnableCorrelation(CorrelationMode enableCorrelation)
    {
        this.enableCorrelation = enableCorrelation;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

}
