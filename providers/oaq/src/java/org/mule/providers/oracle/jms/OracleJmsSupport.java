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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.naming.Context;

import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsSession;
import oracle.jms.AQjmsTopicConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.jms.Jms102bSupport;
import org.mule.providers.jms.JmsConnector;

/**
 * Extends the standard Mule JMS Provider with functionality specific to Oracle's 
 * JMS implementation based on Advanced Queueing (Oracle AQ).  
 * 
 * As of Oracle 9i, only the JMS 1.0.2b specification is supported. 
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @author henks
 * @see OracleJmsConnector
 * @see org.mule.providers.jms.Jms102bSupport
 * @see <a href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96587/toc.htm">Oracle9i Application Developer's Guide - Advanced Queueing</a>
 * @see <a href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96587/jcreate.htm#103729">Oracle9i J2EE Compliance</a>
 */
public class OracleJmsSupport extends Jms102bSupport {

	/** Since we can't access the endpoint's properties directly from the scope of this
	 * class, we save a copy of the properties in this local variable for easy 
	 * reference. */
    private Map endpointProperties;

    public OracleJmsSupport(JmsConnector connector, Context context, 
    						boolean jndiDestinations, boolean forceJndiDestinations) {
        super(connector, context, jndiDestinations, forceJndiDestinations);
    }

    /** 
     * Oracle throws a "JMS-102: Feature not supported" error if any of these
     * "standard" properties are used.
     */
    public boolean supportsProperty(String property) {
    	return (property.equalsIgnoreCase("ReplyTo") == false);
    }
    
	/** The Oracle JMS implementation requires a JDBC Connection to be created prior to 
     * creating the JMS Connection.
     * This method assumes the username and password are included in the JDBC URL.
     * @see OracleJmsConnector#url */
	public Connection createConnection(ConnectionFactory connectionFactory) throws JMSException {
	    return createConnection(connectionFactory, /*username*/null, /*password*/null);
	}   

	/** The Oracle JMS implementation requires a JDBC Connection to be created prior to 
     * creating the JMS Connection.
     * @see OracleJmsConnector#url */
    public javax.jms.Connection createConnection(ConnectionFactory connectionFactory,
									   			 String username, String password) throws JMSException {
    	javax.jms.Connection jmsConnection = null;    	
		java.sql.Connection jdbcConnection = null;
		try {
	    	log.debug("Creating queue/topic connection, URL = " + ((OracleJmsConnector) connector).getUrl());
			if (username == null) {
				jdbcConnection = DriverManager.getConnection(
						((OracleJmsConnector) connector).getUrl());
			} else {
				jdbcConnection = DriverManager.getConnection(
						((OracleJmsConnector) connector).getUrl(), username, password);
			}
		} catch (SQLException e) {
			throw new JMSException("Unable to open JDBC connection", e.getMessage());
		}

		// TODO How do we determine if we are creating a connection for a queue or for 
		// a topic? 
    	if (/*isTopic()*/false) {		
			jmsConnection = AQjmsTopicConnectionFactory.createTopicConnection(jdbcConnection);
    	} else {
    		jmsConnection = AQjmsQueueConnectionFactory.createQueueConnection(jdbcConnection);
		}
    	
    	// Add the connection to the list of open JMS connections maintained by the connector. 
    	// @see org.mule.providers.oracle.jms.OracleJmsConnector#connections */
		((OracleJmsConnector) connector).getConnections().add(jmsConnection);

		return jmsConnection;
	}

    /** In order to receive messages from a queue whose payload is an ADT (Oracle 
     * Advanced Data Type), we must pass the payload factory as a parameter when
     * creating the receiver/subscriber. 
     * @see OracleJmsConnector#PAYLOADFACTORY_PROPERTY */
	public MessageConsumer createConsumer(Session session, Destination destination, 
										  String messageSelector, boolean noLocal,
										  String durableName) throws JMSException {

    	Object payloadFactory = getPayloadFactory();    	
        if (payloadFactory == null) {
        	return super.createConsumer(session, destination, messageSelector, noLocal, durableName);
        } 
        else {
        	if (destination == null) {
        		throw new IllegalArgumentException("Destination (queue or topic) is missing.");
        	}
	        else if ((destination instanceof Queue) && (session instanceof QueueSession)) {
                if (messageSelector != null) {
                    return ((AQjmsSession) session).createReceiver((Queue) destination, messageSelector, payloadFactory);
                } else {
                    return ((AQjmsSession) session).createReceiver((Queue) destination, payloadFactory);
                }
	        }
        	else if ((destination instanceof Topic) && (session instanceof TopicSession)) {
                if (durableName == null) {
                    return ((AQjmsSession) session).createSubscriber((Topic) destination, messageSelector, noLocal);
                } else {
                    return ((AQjmsSession) session).createDurableSubscriber((Topic) destination,
                                                                            messageSelector,
                                                                            durableName,
                                                                            noLocal,
																			payloadFactory);
                }
	        }
	        else throw new IllegalArgumentException("Session and domain type do not match");
        } 
    }

	/**
	 * The standard Oracle JMS classes ({@code oracle.jms}) do not support dynamic 
	 * (i.e., run-time) creation of queues.  This is only possible through the 
	 * (non-standard) administrative classes ({@code oracle.AQ}).  Therefore this method, 
	 * which calls {@code AQjmsSession.createQueue(name)} or 
	 * {@code AQjmsSession.createTopic(name)} will inevitably fail.
	 * 
	 *  The failure <i>should</i> produce a {@code JMSException} but for some reason it
	 *  doesn't (maybe an Oracle bug) and just returns null.  In this case, we generate
	 *  the appropriate exception.
	 */
    public Destination createDestination(Session session, String name, boolean topic) throws JMSException {
    	Destination dest = super.createDestination(session, name, topic);
    	if (dest != null) return dest;
    	else throw new JMSException("Unable to create " + (topic ? "topic" : "queue") + ": " + name);
    }

	/**
	 * The standard Oracle JMS classes ({@code oracle.jms}) do not support dynamic 
	 * (i.e., run-time) creation of queues.  This is only possible through the 
	 * (non-standard) administrative classes ({@code oracle.AQ}).  Therefore this method, 
	 * which calls {@code AQjmsSession.createQueue(name)} or 
	 * {@code AQjmsSession.createTopic(name)} will inevitably fail.
	 * 
	 *  The failure <i>should</i> produce a {@code JMSException} but for some reason it
	 *  doesn't (maybe an Oracle bug) and just returns null.  In this case, we generate
	 *  the appropriate exception.
	 */
    public Destination createTemporaryDestination(Session session, boolean topic) throws JMSException {
    	Destination dest = super.createTemporaryDestination(session, topic);
    	if (dest != null) return dest;
    	else throw new JMSException("Unable to create temporary " + (topic ? "topic" : "queue"));
    }
	
    /** Get the payload factory class, if defined, from the connector or endpoint's 
     * properties. 
     * @see OracleJmsConnector#PAYLOADFACTORY_PROPERTY */
    public Object getPayloadFactory() throws JMSException {
   
    	// Get the global property set on the connector, if any.
    	String payloadFactoryClass = ((OracleJmsConnector) connector).getPayloadFactory();	

    	// If the property has been set for this endpoint, it overrides the global 
    	// setting.
    	if ((endpointProperties != null) 
	    	&& (endpointProperties.get(OracleJmsConnector.PAYLOADFACTORY_PROPERTY) != null)) {
	    	payloadFactoryClass = (String) endpointProperties.get(OracleJmsConnector.PAYLOADFACTORY_PROPERTY);
    	}

    	Object payloadFactory = null;
    	if (payloadFactoryClass != null) {
	    	Throwable ex = null;
        	try { 
        		payloadFactory = Class.forName(payloadFactoryClass).newInstance();
            } catch (ClassNotFoundException e) { ex = e;
            } catch (IllegalAccessException e) { ex = e;
            } catch (InstantiationException e) { ex = e;
            } if (ex != null) throw new JMSException("Unable to instantiate payload factory class " + payloadFactoryClass);
	    }
    	return payloadFactory;
    }

    public Map getEndpointProperties() {
		return endpointProperties;
	}
	public void setEndpointProperties(Map endpointProperties) {
		this.endpointProperties = endpointProperties;
	}

    private static Log log = LogFactory.getLog(OracleJmsSupport.class);
}
