/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.transport.jms.JmsConnector;

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
