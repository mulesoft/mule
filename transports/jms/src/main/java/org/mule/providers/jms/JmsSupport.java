/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * <code>JmsSupport</code> is an interface that provides a polymorphic facade
 * to the Jms 1.0.2b and 1.1 api specifications. this interface is not intended
 * for general purpose use and should only be used with the Mule Jms connector.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface JmsSupport
{
    Connection createConnection(ConnectionFactory connectionFactory) throws JMSException;

    Connection createConnection(ConnectionFactory connectionFactory, String username, String password)
            throws JMSException;

    Session createSession(Connection connection, boolean topic, boolean transacted, int ackMode, boolean noLocal) throws JMSException;

    MessageProducer createProducer(Session session, Destination destination, boolean topic) throws JMSException;

    MessageConsumer createConsumer(Session session,
                                   Destination destination,
                                   String messageSelector,
                                   boolean noLocal,
                                   String durableName,
                                   boolean topic) throws JMSException;

    MessageConsumer createConsumer(Session session, Destination destination, boolean topic) throws JMSException;

    Destination createDestination(Session session, String name, boolean topic) throws JMSException;

    Destination createTemporaryDestination(Session session, boolean topic) throws JMSException;

    void send(MessageProducer producer, Message message, boolean topic) throws JMSException;

    void send(MessageProducer producer, Message message, boolean persistent, int priority, long ttl, boolean topic)
            throws JMSException;

    void send(MessageProducer producer, Message message, Destination dest, boolean topic) throws JMSException;

    void send(MessageProducer producer, Message message, Destination dest, boolean persistent, int priority, long ttl, boolean topic)
            throws JMSException;
}
