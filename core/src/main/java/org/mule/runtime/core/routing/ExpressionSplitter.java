/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
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
    }

    public void initialise() throws InitialisationException
    {
        expressionManager = muleContext.getExpressionManager();
        config.validate(expressionManager);
    }

    @Override
    protected List<MuleEvent> splitMessage(MuleEvent event)
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
            List<MuleEvent> messages = new ArrayList<>();
            for (Object object : (Collection<?>) result)
            {
                messages.add(new DefaultMuleEvent(new DefaultMuleMessage(object, muleContext), event));
            }
            return messages;
        }
        else if (result instanceof Map<?, ?>)
        {
            List<MuleEvent> list = new LinkedList<>();
            Set<Map.Entry<?, ?>> set = ((Map) result).entrySet();
            for (Entry<?, ?> entry : set)
            {
                MuleMessage message = new DefaultMuleMessage(entry.getValue(), muleContext);
                MuleEvent newEvent = new DefaultMuleEvent(message, event);
                newEvent.setFlowVariable(MapSplitter.MAP_ENTRY_KEY, entry.getKey());
                list.add(newEvent);
            }
            return list;
        }
        else if (result instanceof MuleMessage)
        {
            return Collections.singletonList(new DefaultMuleEvent((MuleMessage) result, event));
        }
        else if (result instanceof NodeList)
        {
            NodeList nodeList = (NodeList) result;
            List<MuleEvent> messages = new ArrayList<>(nodeList.getLength());
            for (int i = 0; i < nodeList.getLength(); i++)
            {
                messages.add(new DefaultMuleEvent(new DefaultMuleMessage(nodeList.item(i), muleContext), event));
            }
            return messages;
        }
        else if (result == null)
        {
            return new ArrayList<MuleEvent>();
        }
        else
        {
            logger.info("The expression does not evaluate to a type that can be split: " + result.getClass().getName());
            return Collections.<MuleEvent> singletonList(new DefaultMuleEvent(new DefaultMuleMessage(result, muleContext), event));
        }
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
