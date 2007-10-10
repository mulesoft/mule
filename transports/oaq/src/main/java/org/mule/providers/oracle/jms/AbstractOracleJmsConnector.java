/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.providers.oracle.jms;


import org.mule.config.i18n.CoreMessages;
import org.mule.providers.ConnectException;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsConstants;
import org.mule.providers.jms.JmsMessageUtils;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.lifecycle.InitialisationException;

import java.io.Serializable;
import java.sql.SQLException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.NamingException;

import oracle.jms.AQjmsSession;
import oracle.jms.AdtMessage;
import oracle.xdb.XMLType;

public abstract class AbstractOracleJmsConnector extends JmsConnector
{
    /**
     * If a queue's payload is an ADT (Oracle Advanced Data Type), the appropriate
     * payload factory must be specified in the endpoint's properties. Note: if
     * <u>all</u> queues are of the same payload type, this property may be set
     * globally for the connector instead of for each endpoint.
     */
    public static final String PAYLOADFACTORY_PROPERTY = "payloadFactory";

    protected String payloadFactory = null;
    
    /**
     * Some versions of Oracle do not support more than one JMS session per
     * connection. In this case we need to open a new connection for each session,
     * otherwise we will get the following error:
     * {@code JMS-106: Cannot have more than one open Session on a JMSConnection.}
     */
    private boolean multipleSessionsPerConnection = false;

    public AbstractOracleJmsConnector()
    {
        super();
        registerSupportedProtocol("jms");
    }

    /**
     * The Oracle AQ connector supports both the oaq:// and the jms:// protocols.
     */
    public String getProtocol()
    {
        return "oaq";
    }

    /**
     * The Oracle AQ connector supports both the oaq:// and the jms:// protocols.
     */
    public boolean supportsProtocol(String protocol)
    {
        // The oaq:// protocol handling breaks the model a bit; you do _not_ need to
        // qualify the jms protocol with oaq (oaq:jms://) hence we need to override
        // the
        // default supportsProtocol() method.
        return getProtocol().equalsIgnoreCase(protocol) || super.getProtocol().equalsIgnoreCase(protocol);
    }

    protected void doConnect() throws ConnectException
    {
        try {
            // Set these to false so that the jndiContext will not be used by the
            // JmsSupport classes
            setJndiDestinations(false);
            setForceJndiDestinations(false);

            setJmsSupport(new OracleJmsSupport(this));
        }
        catch (Exception e) {
            throw new ConnectException(CoreMessages.failedToCreate("Oracle Jms Connector"), 
                e, this);
        }

        // Note it doesn't make sense to start a connection at this point
        // (as the standard JMS Provider does) because a connection will be created
        // for
        // each session.
    }

    /**
     * Some versions of Oracle do not support more than one JMS session per
     * connection. In this case we need to open a new connection for each session,
     * otherwise we will get the following error:
     * {@code JMS-106: Cannot have more than one open Session on a JMSConnection.}
     *
     * @see #multipleSessionsPerConnection
     */
    public Session getSession(boolean transacted, boolean topic) throws JMSException {

        if (multipleSessionsPerConnection) {
            return super.getSession(transacted, topic);
        } else {
            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();

            // Check to see if we are already in a session.
            Session session = getSessionFromTransaction();
            if (session != null) {
                return session;
            }

            // Create a new database connection before creating a new session.
            Connection connection;
            try {
                connection = createConnection();
            }
            catch (NamingException e) {
                throw new JMSException("Unable to open new database connection: " + e.getMessage());
            }
            catch (InitialisationException e) {
                throw new JMSException("Unable to open new database connection: " + e.getMessage());
            }

            // Create a new session.
            logger.debug("Retrieving new jms session from connection");
            session = getJmsSupport().createSession(connection, topic, transacted || tx != null,
                    getAcknowledgementMode(), isNoLocal());
            if (tx != null) {
                logger.debug("Binding session " + session + " to current transaction " + tx);
                try {
                    tx.bindResource(connection, session);
                }
                catch (TransactionException e) {
                    throw new RuntimeException("Could not bind session to current transaction", e);
                }
            }
            return session;
        }
    }

    /**
     * Oracle throws a "JMS-102: Feature not supported" error if any of these
     * "standard" properties are used.
     */
    public boolean supportsProperty(String property) {
        return (!JmsConstants.JMS_REPLY_TO.equalsIgnoreCase(property) && !JmsConstants.JMS_TYPE.equalsIgnoreCase(property));
    }

    /**
     * If the incoming message is an XMLType, return it as a standard
     * {@code javax.jms.TextMessage}. If the incoming message is any other
     * AdtMessage, return it as a standard {@code javax.jms.ObjectMessage}.
     */
    public javax.jms.Message preProcessMessage(javax.jms.Message message, Session session) throws Exception {
        Object payload;
        javax.jms.Message newMessage;

        if (message instanceof AdtMessage) {
            payload = ((AdtMessage) message).getAdtPayload();

            if (payload instanceof XMLType) {
                newMessage = session.createTextMessage(((XMLType) payload).getStringVal().trim());
            } else if (payload instanceof Serializable) {
                newMessage = session.createObjectMessage((Serializable) payload);
            } else {
                throw new JMSException("The payload of the incoming AdtMessage must be serializable.");
            }
            // TODO Is there a better way to do this?
            JmsMessageUtils.copyJMSProperties(message, newMessage, this);
            return newMessage;
        } else {
            return message;
        }
    }

    /**
     * Attempts to close the underlying JDBC connection before closing the JMS
     * session.
     *
     * @see org.mule.providers.jms.JmsConnector.close( javax.jms.Session)
     */
    public void close(Session session) throws JMSException {
        if (session != null) {
            java.sql.Connection conn = ((AQjmsSession) session).getDBConnection();
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.commit();
                    conn.close();
                }
            }
            catch (SQLException e) {
                JMSException ex = new JMSException(e.getMessage());
                ex.setLinkedException(e);
                throw ex;
            }
        }
    }

    public abstract java.sql.Connection getJdbcConnection() throws JMSException;

    public boolean isMultipleSessionsPerConnection() {
        return multipleSessionsPerConnection;
    }

    public void setMultipleSessionsPerConnection(boolean multipleSessionsPerConnection) {
        this.multipleSessionsPerConnection = multipleSessionsPerConnection;
    }

    /**
     * Oracle has two different factory classes: {@code AQjmsQueueConnectionFactory}
     * which implements {@code javax.jms.QueueConnectionFactory} and
     * {@code AQjmsTopicConnectionFactory} which implements
     * {@code javax.jms.TopicConnectionFactory} so there is no single class to return
     * in this method.
     *
     * @return null
     */
    protected ConnectionFactory createConnectionFactory() throws InitialisationException, NamingException {
        return null;
    }

    public String getPayloadFactory()
    {
        return payloadFactory;
    }

    public void setPayloadFactory(String payloadFactory)
    {
        this.payloadFactory = payloadFactory;
    }
}
