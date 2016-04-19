/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * <code>JmsSupport</code> is an interface that provides a polymorphic facade to
 * the JMS 1.0.2b and 1.1 API specifications. this interface is not intended for
 * general purpose use and should only be used with the Mule JMS connector.
 */

public interface JmsSupport
{
    Connection createConnection(ConnectionFactory connectionFactory) throws JMSException;

    Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
        throws JMSException;

    Session createSession(Connection connection,
                          boolean topic,
                          boolean transacted,
                          int ackMode,
                          boolean noLocal) throws JMSException;

    MessageProducer createProducer(Session session, Destination destination, boolean topic)
        throws JMSException;

    MessageConsumer createConsumer(Session session,
                                   Destination destination,
                                   String messageSelector,
                                   boolean noLocal,
                                   String durableName,
                                   boolean topic, ImmutableEndpoint endpoint) throws JMSException;

    MessageConsumer createConsumer(Session session, Destination destination, boolean topic, ImmutableEndpoint endpoint)
        throws JMSException;

    Destination createDestination(Session session, String name, boolean topic, ImmutableEndpoint endpoint) throws JMSException;

    Destination createDestination(Session session, ImmutableEndpoint endpoint) throws JMSException;

    Destination createTemporaryDestination(Session session, boolean topic) throws JMSException;

    void send(MessageProducer producer, Message message, boolean topic, ImmutableEndpoint endpoint) throws JMSException;

    void send(MessageProducer producer,
              Message message,
              boolean persistent,
              int priority,
              long ttl,
              boolean topic, ImmutableEndpoint endpoint) throws JMSException;

    void send(MessageProducer producer, Message message, Destination dest, boolean topic, ImmutableEndpoint endpoint) throws JMSException;

    void send(MessageProducer producer,
              Message message,
              Destination dest,
              boolean persistent,
              int priority,
              long ttl,
              boolean topic, ImmutableEndpoint endpoint) throws JMSException;
}
