/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.jms.JmsConnector;

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
    public void setConnector(JmsConnector connector)
    {
        this.connector = connector;
    }
    
    protected MuleMessage createMuleMessage(Message message)
    {
        try
        {
            String encoding = connector.getMuleContext().getConfiguration().getDefaultEncoding();
            return connector.createMuleMessageFactory().create(message, encoding);
        }
        catch (Exception e)
        {
            return new DefaultMuleMessage(message, connector.getMuleContext());
        }
    }
}
