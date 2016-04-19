/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.transport.jms.JmsConnector;

import javax.jms.JMSException;
import javax.jms.Message;

public abstract class AbstractRedeliveryHandler implements RedeliveryHandler
{
    protected JmsConnector connector;

    public abstract void handleRedelivery(Message message, InboundEndpoint endpoint, FlowConstruct flow) throws JMSException, MuleException;

    /**
     * The connector associated with this handler is set before
     * <code>handleRedelivery()</code> is called
     * 
     * @param connector the connector associated with this handler
     */
    @Override
    public void setConnector(JmsConnector connector)
    {
        this.connector = connector;
    }
    
    protected MuleMessage createMuleMessage(Message message, MuleContext muleContext)
    {
        try
        {
            String encoding = muleContext.getConfiguration().getDefaultEncoding();
            return connector.createMuleMessageFactory().create(message, encoding, muleContext);
        }
        catch (Exception e)
        {
            return new DefaultMuleMessage(message, muleContext);
        }
    }
}
