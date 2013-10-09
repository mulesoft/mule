/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.jms.JmsConnector;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * <code>RedeliveryHandler</code> is used to control how redelivered messages are
 * processed by a connector. Typically, a messsage will be re-tried once or twice
 * before throwing an exception. Then the exception strategy on the connector can be
 * used to forward the message to a JMS queue or log the failure.
 */
public interface RedeliveryHandler
{

    /**
     * The connector associated with this handler is set before
     * <code>handleRedelivery()</code> is called
     *
     * @param connector the connector associated with this handler
     */
    public void setConnector(JmsConnector connector);

    /**
     * Process the redelivered message. If the JMS receiver should process the
     * message, it should be returned. Otherwise the connector should throw a
     * {@link MessageRedeliveredException} to indicate that the message should be
     * handled by the connector's exception handler.
     *
     * @param message the redelivered message
     * @param endpoint from which the message was received
     * @param flow in which the exception occured, this is used to obtain the
     *            appropriate exception handler
     * @throws JMSException if properties cannot be read from the JMSMessage
     * @throws MessageRedeliveredException should be thrown if the message should be
     *             handled by the connection exception handler
     * @throws MuleException if there is a problem reading or proessing the message
     */
    public void handleRedelivery(Message message, InboundEndpoint endpoint, FlowConstruct flow) throws JMSException, MuleException;

}
