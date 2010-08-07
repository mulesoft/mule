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

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.routing.ConditionalMessageProcessor;
import org.mule.routing.MessageFilter;
import org.springframework.beans.factory.FactoryBean;

public class ConditionalMessageProcessorFactoryBean implements FactoryBean
{
    private MessageProcessor messageProcessor;
    private Filter filter;

    public void setMessageProcessor(MessageFilter messageFilter)
    {
        this.filter = messageFilter.getFilter();
    }

    public void setEndpoint(OutboundEndpoint endpoint)
    {
        this.messageProcessor = endpoint;
    }

    public Object getObject() throws Exception
    {
        return filter == null
                             ? new ConditionalMessageProcessor(messageProcessor)
                             : new ConditionalMessageProcessor(messageProcessor, filter);
    }

    public Class<?> getObjectType()
    {
        return ConditionalMessageProcessor.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

}
