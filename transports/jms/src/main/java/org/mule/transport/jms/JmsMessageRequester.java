/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.transport.AbstractMessageRequester;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * <code>JmsMessageDispatcher</code> is responsible for dispatching messages to JMS
 * destinations. All JMS semantics apply and settings such as replyTo and QoS
 * properties are read from the event properties or defaults are used (according to
 * the JMS specification)
 */
public class JmsMessageRequester extends AbstractMessageRequester
{

    private JmsConnector connector;

    public JmsMessageRequester(ImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JmsConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        Session session = null;
        MessageConsumer consumer = null;

        try
        {
            final boolean topic = connector.getTopicResolver().isTopic(endpoint);

            JmsSupport support = connector.getJmsSupport();
            session = connector.getSession(false, topic);
            Destination dest = support.createDestination(session, endpoint.getEndpointURI().getAddress(),
                topic);
            consumer = support.createConsumer(session, dest, topic);

            try
            {
                Message message;

                if (timeout == JmsMessageDispatcher.RECEIVE_NO_WAIT)
                {
                    message = consumer.receiveNoWait();
                }
                else if (timeout == JmsMessageDispatcher.RECEIVE_WAIT_INDEFINITELY)
                {
                    message = consumer.receive();
                }
                else
                {
                    message = consumer.receive(timeout);
                }

                if (message == null)
                {
                    return null;
                }

                message = connector.preProcessMessage(message, session);

                return new DefaultMuleMessage(connector.getMessageAdapter(message));
            }
            catch (Exception e)
            {
                connector.handleException(e);
                return null;
            }
        }
        finally
        {
            connector.closeQuietly(consumer);
            connector.closeQuietly(session);
        }
    }

    protected void doDispose()
    {
        // template method
    }

}