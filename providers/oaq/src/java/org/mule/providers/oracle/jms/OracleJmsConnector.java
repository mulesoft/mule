/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.oracle.jms;

import java.io.Serializable;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.NamingException;

import oracle.jdbc.pool.OracleDataSource;
import oracle.jms.AQjmsSession;
import oracle.jms.AdtMessage;
import oracle.xdb.XMLType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.ConnectException;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsConstants;
import org.mule.providers.jms.JmsMessageUtils;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;

/**
 * Extends the standard Mule JMS Provider with functionality specific to Oracle's
 * JMS implementation based on Advanced Queueing (Oracle AQ).
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @author henks
 * @see OracleJmsSupport
 * @see org.mule.providers.jms.JmsConnector
 * @see <a href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96587/toc.htm">Oracle9i Application Developer's Guide - Advanced Queueing</a>
 */
public class OracleJmsConnector extends JmsConnector {

    /** If a queue's payload is an ADT (Oracle Advanced Data Type), the appropriate
     * payload factory must be specified in the endpoint's properties.
     * Note: if <u>all</u> queues are of the same payload type, this property may be
     * set globally for the connector instead of for each endpoint. */
    public static final String PAYLOADFACTORY_PROPERTY = "payloadFactory";

    private String payloadFactory = null;

    /** The JDBC URL for the Oracle database.  For example, {@code jdbc:oracle:oci:@myhost} */
    private String url;

    /** Some versions of Oracle do not support more than one JMS session per connection.
     * In this case we need to open a new connection for each session, otherwise we will
     * get the following error:
     * {@code JMS-106: Cannot have more than one open Session on a JMSConnection.} */
    private boolean multipleSessionsPerConnection = false;

    /** Instead of a single JMS Connection, the Oracle JMS Connector maintains a list of
     * open connections.
     * @see #multipleSessionsPerConnection  */
    private List connections = new ArrayList();

    /** Since many connections are opened and closed, we use a connection pool to
     * obtain the JDBC connection. */
    private OracleDataSource jdbcConnectionPool = null;

    public OracleJmsConnector() {
        super();
        registerSupportedProtocol("jms");
    }

    /**
     * The Oracle AQ connector supports both the oaq:// and the jms:// protocols.
     */
    public String getProtocol() {
        return "oaq";
    }

    /**
     * The Oracle AQ connector supports both the oaq:// and the jms:// protocols.
     */
    public boolean supportsProtocol(String protocol) {
        // The oaq:// protocol handling breaks the model a bit; you do _not_ need to
        // qualify the jms protocol with oaq (oaq:jms://) hence we need to override the
        // default supportsProtocol() method.
        return getProtocol().equalsIgnoreCase(protocol)
                || super.getProtocol().equalsIgnoreCase(protocol);
    }

    /** Oracle has two different factory classes:
     * {@code AQjmsQueueConnectionFactory} which implements {@code javax.jms.QueueConnectionFactory}
     * and {@code AQjmsTopicConnectionFactory} which implements {@code javax.jms.TopicConnectionFactory}
     * so there is no single class to return in this method.
     * @return null */
    protected ConnectionFactory createConnectionFactory() throws InitialisationException, NamingException {
        return null;
    }

    public void doInitialise() throws InitialisationException {
        try {
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());

            jdbcConnectionPool = new OracleDataSource();
            jdbcConnectionPool.setDataSourceName("Mule Oracle AQ Provider");
            jdbcConnectionPool.setUser(username);
            jdbcConnectionPool.setPassword(password);
            jdbcConnectionPool.setURL(url);

        } catch (SQLException e) {
            throw new InitialisationException(e, this);
        }
        super.doInitialise();
    }

    public void doConnect() throws ConnectException {
        try {
            // Set these to false so that the jndiContext will not be used by the
            // JmsSupport classes
            setJndiDestinations(false);
            setForceJndiDestinations(false);

            setJmsSupport(new OracleJmsSupport(this, null, false, false));

        } catch (Exception e) {
            throw new ConnectException(new Message(Messages.FAILED_TO_CREATE_X, "Oracle Jms Connector"), e, this);
        }
    }

    /** Iterate through the open connections and close them one by one. */
//    protected void doDispose() {
//        // From AbstractConnector.doDispose()
//        try {
//            stopConnector();
//        } catch (UMOException e) {
//            logger.warn("Failed to stop during shutdown: " + e.getMessage(), e);
//        }
//
//        try {
//            // Iterate through the open connections and close them one by one.
//            Connection jmsConnection = null;
//            for (Iterator i = connections.iterator(); i.hasNext(); ) {
//                jmsConnection = (Connection) i.next();
//                if (jmsConnection != null) {
//                    jmsConnection.close();
//                    jmsConnection = null;
//                }
//            }
//        } catch (JMSException e) {
//            logger.error("Unable to close Oracle JMS connection: " + e);
//        }
//    }

    /** Iterate through the open connections and start them one by one. */
    public void doStart() throws UMOException {
        Connection jmsConnection = null;
        try {
            // Iterate through the open connections and start them one by one.
            for (Iterator i = connections.iterator(); i.hasNext(); ) {
                jmsConnection = (Connection) i.next();
                if (jmsConnection != null) {
                    jmsConnection.start();
                }
            }
        } catch (JMSException e) {
            throw new LifecycleException(new Message(Messages.FAILED_TO_START_X, "Jms Connection"), e);
        }
    }

    /** Some versions of Oracle do not support more than one JMS session per connection.
     * In this case we need to open a new connection for each session, otherwise we will
     * get the following error:
     * {@code JMS-106: Cannot have more than one open Session on a JMSConnection.}
     * @see #multipleSessionsPerConnection
     * @see #connections */
    public Session getSession(boolean transacted, boolean topic) throws JMSException {

        if (multipleSessionsPerConnection) {
            return super.getSession(transacted, topic);
        }
        else {
            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();

            // Check to see if we are already in a session.
            Session session = getCurrentSession();
            if (session != null) {
                logger.debug("Retrieving jms session from current transaction");
                return session;
            }

            // Create a new database connection before creating a new session.
            Connection connection = null;
            try {
                connection = createConnection();
            } catch (NamingException e) {
                throw new JMSException("Unable to open new database connection.", e.getMessage());
            } catch (InitialisationException e) {
                throw new JMSException("Unable to open new database connection.", e.getMessage());
            }

            // Create a new session.
            logger.debug("Retrieving new jms session from connection");
            session = getJmsSupport().createSession(connection, topic, transacted || tx != null, getAcknowledgementMode(), isNoLocal());
            if (tx != null) {
                logger.debug("Binding session to current transaction");
                try {
                    tx.bindResource(connection, session);
                } catch (TransactionException e) {
                    throw new RuntimeException("Could not bind session to current transaction", e);
                }
            }
            return session;
        }
    }

    /** If {@code multipleSessionsPerConnection} is false, the Oracle JMS Connector
     * should not access the generic JMSConnector {@code connection} property.
     * @see #connections */
    public Connection getConnection() {
        if (multipleSessionsPerConnection) {
            return super.getConnection();
        }
        else {
            log.error("Oracle JMS Connector should not access the generic JMSConnector connection property.");
            return null;
        }
    }

    /** If {@code multipleSessionsPerConnection} is false, the Oracle JMS Connector
     * should not access the generic JMSConnector {@code connection} property.
     * @see #connections */
    protected void setConnection(Connection connection) {
        if (multipleSessionsPerConnection) {
            super.setConnection(connection);
        }
        else {
            log.error("Oracle JMS Connector should not access the generic JMSConnector connection property.");
        }
    }

    public List getConnections() {
        return connections;
    }

    /**
     * Oracle throws a "JMS-102: Feature not supported" error if any of these
     * "standard" properties are used.
     */
    public boolean supportsProperty(String property) {
        return (property.equalsIgnoreCase(JmsConstants.JMS_REPLY_TO) == false
                && property.equalsIgnoreCase(JmsConstants.JMS_TYPE) == false);
    }

    /**
     * If the incoming message is an XMLType, return it as a standard {@code javax.jms.TextMessage}.
     * If the incoming message is any other AdtMessage, return it as a standard {@code javax.jms.ObjectMessage}.
     */
    public javax.jms.Message preProcessMessage(javax.jms.Message message, Session session) throws Exception {
        Object payload;
        javax.jms.Message newMessage;

        if (message instanceof AdtMessage) {
            payload = ((AdtMessage) message).getAdtPayload();

            if (payload instanceof XMLType) {
                newMessage = session.createTextMessage(((XMLType) payload).getStringVal());
            }
            else if (payload instanceof Serializable) {
                newMessage = session.createObjectMessage((Serializable) payload);
            }
            else {
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
     * Close the underlying JDBC connection before closing the JMS session.
     */
    public void close(Session session) throws JMSException {
        if (session != null) {
            try {
                ((AQjmsSession) session).getDBConnection().close();
            } catch (SQLException e) {
                throw new JMSException("Failed to close the Oracle JMS session's underlying JDBC connection: " + e.getMessage());
            }
        }
        super.close(session);
    }

    /**
     * Close the underlying JDBC connection before closing the JMS session.
     */
    public void closeQuietly(Session session) {
        if (session != null) {
            try {
                ((AQjmsSession) session).getDBConnection().close();
                session.close();
            } catch (SQLException e) {
                log.error("Failed to close the Oracle JMS session's underlying JDBC connection", e);
            } catch (JMSException e) {
                log.error("Failed to close the Oracle JMS session's underlying JDBC connection", e);
            }
        }
        super.closeQuietly(session);
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isMultipleSessionsPerConnection() {
        return multipleSessionsPerConnection;
    }
    public void setMultipleSessionsPerConnection(boolean multipleSessionsPerConnection) {
        this.multipleSessionsPerConnection = multipleSessionsPerConnection;
    }

    public String getPayloadFactory() {
        return payloadFactory;
    }
    public void setPayloadFactory(String payloadFactory) {
        this.payloadFactory = payloadFactory;
    }

    public OracleDataSource getJdbcConnectionPool() {
        return jdbcConnectionPool;
    }

    private static Log log = LogFactory.getLog(OracleJmsConnector.class);
}
