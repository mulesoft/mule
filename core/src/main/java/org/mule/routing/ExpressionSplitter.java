/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.expression.ExpressionConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.NodeList;

/**
 * Splits a message using the expression provided invoking the next message processor
 * one for each split part.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www.eaipatterns.com/Sequencer.html</a>
 */
public class ExpressionSplitter extends AbstractSplitter
    implements Initialisable
{
    protected ExpressionManager expressionManager;
    protected ExpressionConfig config = new ExpressionConfig();

    public ExpressionSplitter()
    {
        // Used by spring
    }

    public ExpressionSplitter(ExpressionConfig config)
    {
        this.config = config;
        setEvaluator(config.getEvaluator());
    }

    public void initialise() throws InitialisationException
    {
        expressionManager = muleContext.getExpressionManager();
        config.validate(expressionManager);
    }

    @Override
    protected List<MuleMessage> splitMessage(MuleEvent event)
    {
        Object result = event.getMuleContext()
            .getExpressionManager()
            .evaluate(config.getFullExpression(expressionManager), event);
        if (result instanceof Object[])
        {
            result = Arrays.asList((Object[]) result);
        }
        if (result instanceof Collection<?>)
        {
            List<MuleMessage> messages = new ArrayList<MuleMessage>();
            for (Object object : (Collection<?>) result)
            {
                messages.add(new DefaultMuleMessage(object, muleContext));
            }
            return messages;
        }
        else if (result instanceof Map<?, ?>)
        {
            List<MuleMessage> list = new LinkedList<MuleMessage>();
            Set<Map.Entry<?, ?>> set = ((Map) result).entrySet();
            for (Entry<?, ?> entry : set)
            {
                MuleMessage message = new DefaultMuleMessage(entry.getValue(), muleContext);
                message.setInvocationProperty(MapSplitter.MAP_ENTRY_KEY, entry.getKey());
                list.add(message);
            }
            return list;
        }
        else if (result instanceof MuleMessage)
        {
            return Collections.singletonList((MuleMessage) result);
        }
        else if (result instanceof NodeList)
        {
            NodeList nodeList = (NodeList) result;
            List<MuleMessage> messages = new ArrayList<>(nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++)
            {
                messages.add(new DefaultMuleMessage(nodeList.item(i), muleContext));
            }
            return messages;
        }
        else if (result instanceof Iterator)
        {
            Iterator iterator = (Iterator) result;
            List<MuleMessage> messages = new ArrayList<>();

            while (iterator.hasNext())
            {
                messages.add(new DefaultMuleMessage(iterator.next(), muleContext));
            }

            return messages;
        }
        else if (result == null)
        {
            return new ArrayList<MuleMessage>();
        }
        else
        {
            logger.info("The expression does not evaluate to a type that can be split: " + result.getClass().getName());
            return Collections.<MuleMessage> singletonList(new DefaultMuleMessage(result, muleContext));
        }
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
        if (evaluator != null && evaluator.equals("xpath"))
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

    protected boolean isSequential()
    {
        return true;
    }
}
