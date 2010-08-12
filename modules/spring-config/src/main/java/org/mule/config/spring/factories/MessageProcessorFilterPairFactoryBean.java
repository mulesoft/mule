/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.MessageProcessorFilterPair;
import org.mule.routing.filters.AcceptAllFilter;
import org.mule.routing.filters.ExpressionFilter;

import org.springframework.beans.factory.FactoryBean;

public class MessageProcessorFilterPairFactoryBean implements FactoryBean, MuleContextAware
{
    private MessageProcessor messageProcessor;
    private Filter filter;

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    public void setMessageProcessor(MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }
    
    public void setExpression(String expression)
    {
        this.filter = new ExpressionFilter(expression);
    }

    public Object getObject() throws Exception
    {
        return filter == null
                             ? new MessageProcessorFilterPair(messageProcessor, AcceptAllFilter.INSTANCE)
                             : new MessageProcessorFilterPair(messageProcessor, filter);
    }

    public Class<?> getObjectType()
    {
        return MessageProcessorFilterPair.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
    
    public void setMuleContext(MuleContext context)
    {
        if (messageProcessor != null && messageProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) messageProcessor).setMuleContext(context);
        }
        if (filter != null && filter instanceof MuleContextAware)
        {
            ((MuleContextAware) filter).setMuleContext(context);
        }
    }

}
