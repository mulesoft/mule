/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import oracle.jms.AQjmsConnection;
import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsTopicConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OracleJmsConnection implements TopicConnection, QueueConnection
{

    private TopicConnection topicConnection;
    private QueueConnection queueConnection;

    /**
     * Instead of a single JMS Connection, the Oracle JMS Connector maintains a list
     * of open connections.
     * 
     * @see OracleJmsConnector.multipleSessionsPerConnection
     */
    private List connections;

    /**
     * Holds a reference to the Oracle JMS Connector.
     */
    private OracleJmsConnector connector;

    public OracleJmsConnection(OracleJmsConnector connector)
    {
        this.connector = connector;
        connections = new ArrayList();
    }

    /** Iterate through the open connections and start them one by one. */
    public void start() throws JMSException
    {
        Connection jmsConnection = null;

        for (Iterator i = connections.iterator(); i.hasNext();)
        {
            jmsConnection = (Connection)i.next();
            if (jmsConnection != null)
            {
                jmsConnection.start();
            }
        }
    }

    /** Iterate through the open connections and stop them one by one. */
    public void stop() throws JMSException
    {
        Connection jmsConnection = null;

        for (Iterator i = connections.iterator(); i.hasNext();)
        {
            jmsConnection = (Connection)i.next();
            if (jmsConnection != null)
            {
                jmsConnection.stop();
            }
        }
    }

    /** Iterate through the open connections and close them one by one. */
    public void close() throws JMSException
    {
        Connection jmsConnection;

        // Iterate through the open connections
        for (Iterator i = connections.iterator(); i.hasNext();)
        {
            jmsConnection = (Connection)i.next();
            if (jmsConnection != null)
            {
                try
                {
                    // Close the JMS Session (and its underlying JDBC connection).
                    connector.close(((AQjmsConnection)jmsConnection).getCurrentJmsSession());
                    // Close the JMS Connection.
                    jmsConnection.close();
                }
                catch (JMSException e)
                {
                    logger.error("Unable to close Oracle JMS connection: " + e.getMessage());
                }
            }
        }
    }

    protected QueueConnection getQueueConnection() throws JMSException
    {
        QueueConnection connection;

        if (connector.isMultipleSessionsPerConnection())
        {
            if (queueConnection == null)
            {
                queueConnection = AQjmsQueueConnectionFactory.createQueueConnection(connector.getJdbcConnection());
                queueConnection.start();
            }
            connection = queueConnection;
        }
        else
        {
            connection = AQjmsQueueConnectionFactory.createQueueConnection(connector.getJdbcConnection());
            connection.start();
            // Add the connection to the list of open JMS connections.
            connections.add(connection);
        }
        return connection;
    }

    protected TopicConnection getTopicConnection() throws JMSException
    {
        TopicConnection connection;

        if (connector.isMultipleSessionsPerConnection())
        {
            if (topicConnection == null)
            {
                topicConnection = AQjmsTopicConnectionFactory.createTopicConnection(connector.getJdbcConnection());
                topicConnection.start();
            }
            connection = topicConnection;
        }
        else
        {
            connection = AQjmsTopicConnectionFactory.createTopicConnection(connector.getJdbcConnection());
            connection.start();
            // Add the connection to the list of open JMS connections.
            connections.add(connection);
        }
        connection.start();
        return connection;
    }

    public QueueSession createQueueSession(boolean transacted, int ackMode) throws JMSException
    {
        return getQueueConnection().createQueueSession(transacted, ackMode);
    }

    public TopicSession createTopicSession(boolean transacted, int ackMode) throws JMSException
    {
        return getTopicConnection().createTopicSession(transacted, ackMode);
    }

    public ConnectionConsumer createConnectionConsumer(Topic arg0,
                                                       String arg1,
                                                       ServerSessionPool arg2,
                                                       int arg3) throws JMSException
    {
        return getTopicConnection().createConnectionConsumer(arg0, arg1, arg2, arg3);
    }

    public ConnectionConsumer createDurableConnectionConsumer(Topic arg0,
                                                              String arg1,
                                                              String arg2,
                                                              ServerSessionPool arg3,
                                                              int arg4) throws JMSException
    {
        return getTopicConnection().createDurableConnectionConsumer(arg0, arg1, arg2, arg3, arg4);
    }

    public ConnectionConsumer createConnectionConsumer(Queue arg0,
                                                       String arg1,
                                                       ServerSessionPool arg2,
                                                       int arg3) throws JMSException
    {
        return getQueueConnection().createConnectionConsumer(arg0, arg1, arg2, arg3);
    }

    // TODO How do we know which connection to use?
    public String getClientID() throws JMSException
    {
        return null;
    }

    // TODO How do we know which connection to use?
    public ExceptionListener getExceptionListener() throws JMSException
    {
        return null;
    }

    // TODO How do we know which connection to use?
    public ConnectionMetaData getMetaData() throws JMSException
    {
        return null;
    }

    public void setClientID(String arg0) throws JMSException
    {
        // TODO How do we know which connection to use?
    }

    public void setExceptionListener(ExceptionListener arg0) throws JMSException
    {
        // TODO How do we know which connection to use?
    }

    private static Log logger = LogFactory.getLog(OracleJmsConnection.class);
}
