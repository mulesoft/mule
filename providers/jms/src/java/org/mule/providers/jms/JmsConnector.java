/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.jms;


import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.mule.InitialisationException;
import org.mule.providers.ReplyToHandler;
import org.mule.providers.TransactionEnabledConnector;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Map;


/**
 * <code>JmsConnector</code> is a JMS 1.0.2b compliant connector that can be used by a Mule
 * endpoint.  The connector supports all Jms functionality including, topics and queues, durable
 * subscribers, acknowledgement modes, loacal transactions
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */


public class JmsConnector extends TransactionEnabledConnector
{

    public static final String JMS_SELECTOR_PROPERTY = "selector";
    public static final String JMS_SPECIFICATION_102B = "1.0.2b";
    public static final String JMS_SPECIFICATION_11 = "1.1";

    private String connectionFactoryJndiName;

    private ConnectionFactory connectionFactory;

    private String jndiInitialFactory;

    private String jndiProviderUrl;

    private int acknowledgementMode = Session.AUTO_ACKNOWLEDGE;

    private String durableName;

    private boolean durable;

    private boolean noLocal;

    private boolean persistentDelivery;

    private Map providerProperties;

    private Connection connection;

    private String specification = JMS_SPECIFICATION_102B;

    private JmsSupport jmsSupport;

    private Context jndiContext;

    private boolean jndiDestinations = false;

    private boolean forceJndiDestinations = false;

    public String username = null;

    public String password = null;

    public JmsConnector()
    {
        receivers = new ConcurrentHashMap();
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#create(java.util.HashMap)
     */
    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        try
        {
            //If we have a connection factory, there is no need to initialise
            //the JndiContext
            if(connectionFactory==null || (connectionFactory!=null && jndiInitialFactory!=null)) {
                initJndiContext();
            } else {
                //Set these to false so that the jndiContext
                //will not be used by the JmsSupport classes
                jndiDestinations = false;
                forceJndiDestinations = false;
            }

            if(JMS_SPECIFICATION_102B.equals(specification)) {
                jmsSupport = new Jms102bSupport(jndiContext, jndiDestinations, forceJndiDestinations);
            } else {
                jmsSupport = new Jms11Support(jndiContext, jndiDestinations, forceJndiDestinations);
            }
            connection = createConnection();
        } catch (Exception e)
        {
            throw new InitialisationException("Failed to create Jms Connector: " + e.getMessage(), e);
        }
    }

    protected void initJndiContext() throws NamingException
    {
        if(jndiContext==null) {
            Hashtable props = new Hashtable();

            if (jndiInitialFactory != null)
                props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);

            if (jndiProviderUrl != null)
                props.put(Context.PROVIDER_URL, jndiProviderUrl);

            if (providerProperties != null)
            {
                props.putAll(providerProperties);
            }
            jndiContext = new InitialContext(props);
        }
    }

    protected void setConnection(Connection connection) {
        this.connection = connection;
    }

    protected ConnectionFactory createConnectionFactory() throws InitialisationException, NamingException
    {

        Object temp =  jndiContext.lookup(connectionFactoryJndiName);

        if(temp instanceof ConnectionFactory)
        {
            return (ConnectionFactory)temp;
        } else {
            throw new InitialisationException("No Connection factory was found for name: " + connectionFactoryJndiName);
        }
    }

    protected Connection createConnection() throws NamingException, JMSException, InitialisationException
    {
        Connection connection = null;
        if(connectionFactory==null)
        {
            connectionFactory = createConnectionFactory();
        }

        if(username!=null) {
            connection = jmsSupport.createConnection(connectionFactory, username, password);
        } else {
            connection = jmsSupport.createConnection(connectionFactory);
        }

        return connection;
    }

    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        return component.getDescriptor().getName() + "~" + endpoint.getEndpointURI().getAddress();
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        Session session = getSession(endpoint.getTransactionConfig().isTransacted());
        boolean topic = false;

        String resourceInfo = endpoint.getEndpointURI().getResourceInfo();
        topic = (resourceInfo!=null && "topic".equalsIgnoreCase(resourceInfo));

        Destination dest = jmsSupport.createDestination(session, endpoint.getEndpointURI().getAddress(), topic);

        MessageConsumer consumer = null;
        String selector=null;

        //Set the slector
        if(endpoint.getFilter()!=null && endpoint.getFilter() instanceof JmsSelectorFilter) {
            selector = ((JmsSelectorFilter)endpoint.getFilter()).getExpression();
        }else if(endpoint.getProperties() != null) {
            //still allow the selector to be set as a property on the endpoint
            //to be backward compatable
            selector = (String)endpoint.getProperties().get(JMS_SELECTOR_PROPERTY);
        }

        if(durable) {
            consumer = jmsSupport.createConsumer(session, dest, selector, noLocal, durableName);
        } else {
            consumer = jmsSupport.createConsumer(session, dest, selector, noLocal, null);
        }
        UMOMessageReceiver receiver = serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[]{consumer, session});
        return receiver;
    }

    Session getSession(boolean transacted) throws JMSException
    {
        Session session = jmsSupport.createSession(connection, transacted, acknowledgementMode, noLocal);
        return session;
    }

    public void stopConnector() throws UMOException
    {
        try
        {
            connection.stop();
        }
        catch (JMSException e)
        {
            throw new InitialisationException("Failed to stop Jms Connection", e);
        }
    }


    public void startConnector() throws UMOException
    {
        try
        {
            connection.start();
        }
        catch (JMSException e)
        {
            throw new InitialisationException("Failed to start Jms Connection", e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#getProtocol()
     */
    public String getProtocol()
    {
        return "JMS";
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#disposeConnector()
     */
    protected void disposeConnector() throws UMOException
    {
        try
        {
            if(connection!=null) connection.close();
        }
        catch (JMSException e)
        {
            logger.error("Jms connector failed to close properly: " + e);
        }
    }


    /**
     * @return Returns the acknowledgeMode.
     */
    public int getAcknowledgementMode()
    {
        return acknowledgementMode;
    }


    /**
     * @param acknowledgementMode The acknowledgementMode to set.
     */
    public void setAcknowledgementMode(int acknowledgementMode)
    {
        this.acknowledgementMode = acknowledgementMode;
    }

    /**
     * @return Returns the connectionFactoryJndiName.
     */
    public String getConnectionFactoryJndiName()
    {
        return connectionFactoryJndiName;
    }


    /**
     * @param connectionFactoryJndiName The connectionFactoryJndiName to set.
     */
    public void setConnectionFactoryJndiName(String connectionFactoryJndiName)
    {
        this.connectionFactoryJndiName = connectionFactoryJndiName;
    }

    /**
     * @return Returns the durable.
     */
    public boolean isDurable()
    {
        return durable;
    }

    /**
     * @param durable The durable to set.
     */
    public void setDurable(boolean durable)
    {
        this.durable = durable;
    }

    /**
     * @return Returns the durableName.
     */
    public String getDurableName()
    {
        return durableName;
    }


    /**
     * @param durableName The durableName to set.
     */
    public void setDurableName(String durableName)
    {
        this.durableName = durableName;
    }

    /**
     * @return Returns the noLocal.
     */
    public boolean isNoLocal()
    {
        return noLocal;
    }


    /**
     * @param noLocal The noLocal to set.
     */
    public void setNoLocal(boolean noLocal)
    {
        this.noLocal = noLocal;
    }


    /**
     * @return Returns the persistentDelivery.
     */
    public boolean isPersistentDelivery()
    {
        return persistentDelivery;
    }


    /**
     * @param persistentDelivery The persistentDelivery to set.
     */
    public void setPersistentDelivery(boolean persistentDelivery)
    {
        this.persistentDelivery = persistentDelivery;
    }

    /**
     * @return Returns the endpointProperties.
     */
    public Map getProviderProperties()
    {
        return providerProperties;
    }


    /**
     * @param endpointProperties The endpointProperties to set.
     */
    public void setProviderProperties(Map endpointProperties)
    {
        this.providerProperties = endpointProperties;
    }

    /**
     * @return Returns the isQueue.
     */
    public boolean isQueue()
    {
        return ((connection instanceof QueueConnection ||
                connection instanceof XAQueueConnection));
    }

    public String getJndiInitialFactory()
    {
        return jndiInitialFactory;
    }

    public void setJndiInitialFactory(String jndiInitialFactory)
    {
        this.jndiInitialFactory = jndiInitialFactory;
    }

    public String getJndiProviderUrl()
    {
        return jndiProviderUrl;
    }

    public void setJndiProviderUrl(String jndiProviderUrl)
    {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    public Object getSession(UMOEndpoint endpoint) throws Exception
    {
        if(endpoint.getTransactionConfig().getFactory() instanceof JmsClientAcknowledgeTransactionFactory) {
            return getSession(false);
        } else {
            return getSession(endpoint.getTransactionConfig().isTransacted());
        }
    }

    public ConnectionFactory getConnectionFactory()
    {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }

    public JmsSupport getJmsSupport()
    {
        return jmsSupport;
    }

    public void setJmsSupport(JmsSupport jmsSupport)
    {
        this.jmsSupport = jmsSupport;
    }

    public String getSpecification()
    {
        return specification;
    }

    public void setSpecification(String specification)
    {
        this.specification = specification;
    }

    public boolean isJndiDestinations()
    {
        return jndiDestinations;
    }

    public void setJndiDestinations(boolean jndiDestinations)
    {
        this.jndiDestinations = jndiDestinations;
    }

    public boolean isForceJndiDestinations()
    {
        return forceJndiDestinations;
    }

    public void setForceJndiDestinations(boolean forceJndiDestinations)
    {
        this.forceJndiDestinations = forceJndiDestinations;
    }

    public Context getJndiContext()
    {
        return jndiContext;
    }

    public void setJndiContext(Context jndiContext)
    {
        this.jndiContext = jndiContext;
    }

    public ReplyToHandler getReplyToHandler()
    {
        try
        {
            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
            Session session;
            if(tx!=null && tx instanceof JmsTransaction) {
                session = (Session)tx.getResource();
            } else {
                session = getSession(false);
            }
            return new JmsReplyToHandler(this, session, defaultResponseTransformer);
        } catch (JMSException e)
        {
            logger.error("failed to get replyTo handler: " + e.getMessage(), e);
            return null;
        }
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

}
