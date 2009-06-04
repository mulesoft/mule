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

import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.notification.ConnectionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.StartException;
import org.mule.api.lifecycle.StopException;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.NotificationException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.jms.i18n.JmsMessages;
import org.mule.transport.jms.xa.ConnectionFactoryWrapper;
import org.mule.util.BeanUtils;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.XAConnectionFactory;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>JmsConnector</code> is a JMS 1.0.2b compliant connector that can be used
 * by a Mule endpoint. The connector supports all JMS functionality including topics
 * and queues, durable subscribers, acknowledgement modes and local transactions.
 */

public class JmsConnector extends AbstractConnector implements ConnectionNotificationListener, ExceptionListener
{

    public static final String JMS = "jms";

    private AtomicInteger receiverReportedExceptionCount = new AtomicInteger();
    
    ////////////////////////////////////////////////////////////////////////
    // Properties
    ////////////////////////////////////////////////////////////////////////

    private int acknowledgementMode = Session.AUTO_ACKNOWLEDGE;

    private String clientId;

    private boolean durable;

    private boolean noLocal;

    private boolean persistentDelivery;

    private boolean honorQosHeaders;

    private int maxRedelivery = 0;

    private boolean cacheJmsSessions = false;

    /** Whether to create a consumer on connect. */
    private boolean eagerConsumer = true;

    ////////////////////////////////////////////////////////////////////////
    // JMS Connection
    ////////////////////////////////////////////////////////////////////////

    /**
     * JMS Connection, not settable by the user.
     */
    private Connection connection;

    private ConnectionFactory connectionFactory;
    
    private Map connectionFactoryProperties;

    public String username = null;

    public String password = null;

    ////////////////////////////////////////////////////////////////////////
    // JNDI Connection
    ////////////////////////////////////////////////////////////////////////
    
    private Context jndiContext = null;

    /**
     * This object guards all access to the jndiContext
     */
    private final Object jndiLock = new Object();

    private String jndiProviderUrl;

    private String jndiInitialFactory;

    private Map jndiProviderProperties;

    private String connectionFactoryJndiName;

    private boolean jndiDestinations = false;

    private boolean forceJndiDestinations = false;

    ////////////////////////////////////////////////////////////////////////
    // Strategy classes
    ////////////////////////////////////////////////////////////////////////

    private String specification = JmsConstants.JMS_SPECIFICATION_102B;

    private JmsSupport jmsSupport;

    private JmsTopicResolver topicResolver;

    private RedeliveryHandlerFactory redeliveryHandlerFactory;

    /** determines whether a temporary JMSReplyTo destination will be used when using synchronous outbound JMS endpoints */
    private boolean disableTemporaryReplyToDestinations = false;

    /**
     * In-container embedded mode disables some features for strict Java EE compliance.
     */
    private boolean embeddedMode;

    ////////////////////////////////////////////////////////////////////////
    // Methods
    ////////////////////////////////////////////////////////////////////////

    /* Register the Jms Exception reader if this class gets loaded */
    static
    {
        ExceptionHelper.registerExceptionReader(new JmsExceptionReader());
    }

    public String getProtocol()
    {
        return JMS;
    }

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            connectionFactory = this.createConnectionFactory();
        }
        catch (NamingException ne)
        {
            throw new InitialisationException(JmsMessages.errorCreatingConnectionFactory(), ne, this);
        }
        
        if ((connectionFactoryProperties != null) && !connectionFactoryProperties.isEmpty())
        {
            // apply connection factory properties
            BeanUtils.populateWithoutFail(connectionFactory, connectionFactoryProperties, true);
        }
        
        if (topicResolver == null)
        {
            topicResolver = new DefaultJmsTopicResolver(this);
        }
        if (redeliveryHandlerFactory == null)
        {
            redeliveryHandlerFactory = new AutoDiscoveryRedeliveryHandlerFactory(this);
        }

        try
        {
            muleContext.registerListener(this, getName());
        }
        catch (NotificationException nex)
        {
            throw new InitialisationException(nex, this);
        }

        if (jmsSupport == null)
        {
            jmsSupport = createJmsSupport();
        }
    }

    /**
     * A factory method to create various JmsSupport class versions.
     * @return JmsSupport instance
     * @see JmsSupport
     */
    protected JmsSupport createJmsSupport()
    {
        final JmsSupport result;
        if (JmsConstants.JMS_SPECIFICATION_102B.equals(specification))
        {
            result = new Jms102bSupport(this);
        }
        else
        {
            result = new Jms11Support(this);
        }

        return result;
    }

    protected ConnectionFactory createConnectionFactory() throws InitialisationException, NamingException
    {
        // if an initial factory class was configured that takes precedence over the 
        // spring-configured connection factory or the one that our subclasses may provide
        if (jndiInitialFactory != null)
        {
            this.initJndiContext();

            Object temp = jndiContext.lookup(connectionFactoryJndiName);
            if (temp instanceof ConnectionFactory)
            {
                return (ConnectionFactory)temp;
            }
            else
            {
                throw new InitialisationException(
                    JmsMessages.invalidResourceType(ConnectionFactory.class, temp), this);
            }
        }
        else
        {
            // don't look up objects from JNDI in any case
            jndiDestinations = false;
            forceJndiDestinations = false;

            // don't use JNDI. Use the spring-configured connection factory if that's provided
            if (connectionFactory != null)
            {
                return connectionFactory;
            }
            
            // no spring-configured connection factory. See if there is a default one (e.g. from
            // subclass)
            ConnectionFactory factory = this.getDefaultConnectionFactory();
            if (factory != null)
            {
                return factory;
            }
            
            // no connection factory ... give up
            throw new InitialisationException(JmsMessages.noConnectionFactoryConfigured(), this);
        }
    }
    
    /** 
     * Override this method to provide a default ConnectionFactory for a vendor-specific JMS Connector. 
     */
    protected ConnectionFactory getDefaultConnectionFactory()
    {
        return null;
    }

    protected void doDispose()
    {
        if (connection != null)
        {
            try
            {
                connection.close();
            }
            catch (JMSException e)
            {
                logger.error("Jms connector failed to dispose properly: ", e);
            }
            connection = null;
        }
        
        if (jndiContext != null)
        {
            try
            {
                jndiContext.close();
            }
            catch (NamingException ne)
            {
                logger.error("Jms connector failed to dispose properly: ", ne);
            }
            finally
            {
                jndiContext = null;
            }
        }
    }

    protected void initJndiContext() throws NamingException, InitialisationException
    {
        synchronized (jndiLock)
        {
            Hashtable<String, Object> props = new Hashtable<String, Object>();

            if (jndiInitialFactory != null)
            {
                props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);
            }
            else if (jndiProviderProperties == null
                     || !jndiProviderProperties.containsKey(Context.INITIAL_CONTEXT_FACTORY))
            {
                throw new InitialisationException(CoreMessages.objectIsNull("jndiInitialFactory"), this);
            }

            if (jndiProviderUrl != null)
            {
                props.put(Context.PROVIDER_URL, jndiProviderUrl);
            }

            if (jndiProviderProperties != null)
            {
                props.putAll(jndiProviderProperties);
            }
            
            jndiContext = new InitialContext(props);
        }
    }

    protected Object lookupFromJndi(String jndiName) throws NamingException
    {
        synchronized (jndiLock)
        {
            try
            {
                return jndiContext.lookup(jndiName);
            }
            catch (CommunicationException ce)
            {
                logger.warn("JNDI communication error", ce);
                
                // Our connection to JNDI failed. Make a single attempt to reconnect to JNDI.
                try
                {
                    /*
                     Uncomment for manual testing ... this gives you time to restart the JNDI
                     server

                    try
                    {
                        logger.info("sleep for 20 secs before JNDI retry");
                        Thread.sleep(20000);
                        logger.info("done sleeping");
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                    */

                    // re-connect to JNDI
                    this.initJndiContext();
                    
                    // now retry the lookup.
                    return jndiContext.lookup(jndiName);
                }
                catch (InitialisationException ie)
                {
                    // this may actually never happen as we were connected to JNDI before
                    throw new MuleRuntimeException(JmsMessages.errorInitializingJndi(), ie);
                }
            }
        }
    }

    protected Connection createConnection() throws NamingException, JMSException, InitialisationException
    {
        ConnectionFactory cf = this.connectionFactory;
        Connection connection;

        try
        {
            if (cf instanceof XAConnectionFactory && muleContext.getTransactionManager() != null)
            {
                cf = new ConnectionFactoryWrapper(cf);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

        if (username != null)
        {
            connection = jmsSupport.createConnection(cf, username, password);
        }
        else
        {
            connection = jmsSupport.createConnection(cf);
        }

        if (connection != null)
        {
            if (clientId != null)
            {
                connection.setClientID(getClientId());
            }
            if (!embeddedMode)
            {
                connection.setExceptionListener(this);
            }
        }


        return connection;
    }

    public void onException(JMSException jmsException)
    {
        final JmsConnector jmsConnector = JmsConnector.this;
        Map receivers = jmsConnector.getReceivers();
        boolean isMultiConsumerReceiver = false;
        
        if (!receivers.isEmpty()) 
        {
            Map.Entry entry = (Map.Entry) receivers.entrySet().iterator().next();
            if (entry.getValue() instanceof MultiConsumerJmsMessageReceiver)
            {
                isMultiConsumerReceiver = true;
            }
        }
        
        int expectedReceiverCount = isMultiConsumerReceiver ? 1 : 
            (jmsConnector.getReceivers().size() * jmsConnector.getNumberOfConcurrentTransactedReceivers());
        
        if (logger.isDebugEnabled())
        {
            logger.debug("About to recycle myself due to remote JMS connection shutdown but need "
                + "to wait for all active receivers to report connection loss. Receiver count: " 
                + (receiverReportedExceptionCount.get() + 1) + '/' + expectedReceiverCount);
        }
        
        if (receiverReportedExceptionCount.incrementAndGet() >= expectedReceiverCount)
        {
            receiverReportedExceptionCount.set(0);
        
            handleException(new ConnectException(jmsException, this));
        }
    }

      // TODO This might make retry work a bit better w/ JMS
//    @Override
//    public boolean validateConnection() throws Exception
//    {
//        logger.debug("Creating a temporary session to verify that we have a healthy connection...");
//
//        Connection connection;
//        Session session;
//        try
//        {
//            connection = createConnection();
//            if (connection == null)
//            {
//                return false;
//            }
//            session = connection.createSession(false, 1);
//            if (session == null)
//            {
//                return false;
//            }
//            session.close();
//            connection.close();
//            return true;
//        }
//        finally
//        {
//            session = null;
//            connection = null;
//        }
//    }
    
    @Override
    protected void doConnect() throws Exception
    {
        connection = createConnection();
        if (started.get())
        {
            connection.start();
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        try
        {
            if (connection != null)
            {
                // Ignore exceptions while closing the connection
                if (!embeddedMode)
                {
                    connection.setExceptionListener(null);
                }
                connection.close();
            }
        }
        finally
        {
            // connectionFactory = null;
            connection = null;
        }
    }

    public MessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        JmsMessageAdapter adapter = (JmsMessageAdapter) super.getMessageAdapter(message);
        adapter.setSpecification(this.getSpecification());
        return adapter;
    }

    protected Object getReceiverKey(Service service, InboundEndpoint endpoint)
    {
        return service.getName() + "~" + endpoint.getEndpointURI().getAddress();
    }

    public Session getSessionFromTransaction()
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            if (tx.hasResource(connection))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Retrieving jms session from current transaction " + tx);
                }

                Session session = (Session) tx.getResource(connection);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Using " + session + " bound to transaction " + tx);
                }

                return session;
            }
        }
        return null;
    }

    public Session getSession(ImmutableEndpoint endpoint) throws JMSException
    {
        final boolean topic = getTopicResolver().isTopic(endpoint);
        return getSession(endpoint.getTransactionConfig().isTransacted(), topic);
    }

    public Session getSession(boolean transacted, boolean topic) throws JMSException
    {
        Session session = getSessionFromTransaction();
        if (session != null)
        {
            return session;
        }

        Transaction tx = TransactionCoordination.getInstance().getTransaction();

        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format(
                    "Retrieving new jms session from connection: " +
                            "topic={0}, transacted={1}, ack mode={2}, nolocal={3}",
                            topic, transacted, acknowledgementMode, noLocal));
        }

        session = jmsSupport.createSession(connection, topic, transacted, acknowledgementMode, noLocal);
        if (tx != null)
        {
            logger.debug("Binding session " + session + " to current transaction " + tx);
            try
            {
                tx.bindResource(connection, session);
            }
            catch (TransactionException e)
            {
                closeQuietly(session);
                throw new RuntimeException("Could not bind session to current transaction", e);
            }
        }
        return session;
    }

    protected void doStart() throws MuleException
    {
        //TODO: This should never be null or an exception should be thrown
        if (connection != null)
        {
            try
            {
                connection.start();
            }
            catch (JMSException e)
            {
                throw new StartException(CoreMessages.failedToStart("Jms Connection"), e, this);
            }
        }
    }

    protected void doStop() throws MuleException
    {
        if (connection != null)
        {
            try
            {
                connection.stop();
            }
            catch (JMSException e)
            {
                throw new StopException(CoreMessages.failedToStop("Jms Connection"), e, this);
            }
        }
    }

    public ReplyToHandler getReplyToHandler()
    {
        return new JmsReplyToHandler(this, getDefaultResponseTransformers());
    }

    public void onNotification(ServerNotification notification)
    {
        if (notification.getAction() == ConnectionNotification.CONNECTION_DISCONNECTED
                || notification.getAction() == ConnectionNotification.CONNECTION_FAILED)
        {
            // Remove all dispatchers as any cached session will be invalidated
            disposeDispatchers();
            // TODO should we dispose receivers here as well (in case they are
            // transactional)
            // gives a harmless NPE at
            // AbstractConnector.connect(AbstractConnector.java:927)
            // disposeReceivers();
        }
    }

    /**
     * This method may be overridden in case a certain JMS implementation does not
     * support all the standard JMS properties.
     */
    public boolean supportsProperty(String property)
    {
        return true;
    }

    /**
     * This method may be overridden in order to apply pre-processing to the message
     * as soon as it arrives.
     *
     * @param message - the incoming message
     * @param session - the JMS session
     * @return the preprocessed message
     */
    public javax.jms.Message preProcessMessage(javax.jms.Message message, Session session) throws Exception
    {
        return message;
    }

    /**
     * Closes the MessageProducer
     *
     * @param producer
     * @throws JMSException
     */
    public void close(MessageProducer producer) throws JMSException
    {
        if (producer != null)
        {
            producer.close();
        }
    }

    /**
     * Closes the MessageProducer without throwing an exception (an error message is
     * logged instead).
     *
     * @param producer
     */
    public void closeQuietly(MessageProducer producer)
    {
        try
        {
            close(producer);
        }
        catch (JMSException e)
        {
            logger.error("Failed to close jms message producer", e);
        }
    }

    /**
     * Closes the MessageConsumer
     *
     * @param consumer
     * @throws JMSException
     */
    public void close(MessageConsumer consumer) throws JMSException
    {
        if (consumer != null)
        {
            consumer.close();
        }
    }

    /**
     * Closes the MessageConsumer without throwing an exception (an error message is
     * logged instead).
     *
     * @param consumer
     */
    public void closeQuietly(MessageConsumer consumer)
    {
        try
        {
            close(consumer);
        }
        catch (JMSException e)
        {
            logger.error("Failed to close jms message consumer", e);
        }
    }

    /**
     * Closes the MuleSession
     *
     * @param session
     * @throws JMSException
     */
    public void close(Session session) throws JMSException
    {
        if (session != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Closing session " + session);
            }
            session.close();
        }
    }

    /**
     * Closes the MuleSession without throwing an exception (an error message is logged
     * instead).
     *
     * @param session
     */
    public void closeQuietly(Session session)
    {
        try
        {
            close(session);
        }
        catch (JMSException e)
        {
            logger.warn("Failed to close jms session consumer", e);
        }
    }

    /**
     * Closes the TemporaryQueue
     *
     * @param tempQueue
     * @throws JMSException
     */
    public void close(TemporaryQueue tempQueue) throws JMSException
    {
        if (tempQueue != null)
        {
            tempQueue.delete();
        }
    }

    /**
     * Closes the TemporaryQueue without throwing an exception (an error message is
     * logged instead).
     *
     * @param tempQueue
     */
    public void closeQuietly(TemporaryQueue tempQueue)
    {
        try
        {
            close(tempQueue);
        }
        catch (JMSException e)
        {
            if (logger.isWarnEnabled())
            {
                String queueName = "";
                try
                {
                    queueName = tempQueue.getQueueName();
                }
                catch (JMSException innerEx)
                {
                    // ignore, we are just trying to get the queue name
                }
                logger.warn(MessageFormat.format(
                        "Failed to delete a temporary queue ''{0}'' Reason: {1}",
                        queueName, e.getMessage()));
            }
        }
    }

    /**
     * Closes the TemporaryTopic
     *
     * @param tempTopic
     * @throws JMSException
     */
    public void close(TemporaryTopic tempTopic) throws JMSException
    {
        if (tempTopic != null)
        {
            tempTopic.delete();
        }
    }

    /**
     * Closes the TemporaryTopic without throwing an exception (an error message is
     * logged instead).
     *
     * @param tempTopic
     */
    public void closeQuietly(TemporaryTopic tempTopic)
    {
        try
        {
            close(tempTopic);
        }
        catch (JMSException e)
        {
            if (logger.isWarnEnabled())
            {
                String topicName = "";
                try
                {
                    topicName = tempTopic.getTopicName();
                }
                catch (JMSException innerEx)
                {
                    // ignore, we are just trying to get the topic name
                }
                logger.warn("Failed to delete a temporary topic " + topicName, e);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ////////////////////////////////////////////////////////////////////////

    /** @return Returns the connection. */
    public Connection getConnection()
    {
        return connection;
    }

    protected void setConnection(Connection connection)
    {
        this.connection = connection;
    }

    /** @return Returns the acknowledgeMode. */
    public int getAcknowledgementMode()
    {
        return acknowledgementMode;
    }

    /** @param acknowledgementMode The acknowledgementMode to set. */
    public void setAcknowledgementMode(int acknowledgementMode)
    {
        this.acknowledgementMode = acknowledgementMode;
    }

    /** @return Returns the durable. */
    public boolean isDurable()
    {
        return durable;
    }

    /** @param durable The durable to set. */
    public void setDurable(boolean durable)
    {
        this.durable = durable;
    }

    /** @return Returns the noLocal. */
    public boolean isNoLocal()
    {
        return noLocal;
    }

    /** @param noLocal The noLocal to set. */
    public void setNoLocal(boolean noLocal)
    {
        this.noLocal = noLocal;
    }

    /** @return Returns the persistentDelivery. */
    public boolean isPersistentDelivery()
    {
        return persistentDelivery;
    }

    /** @param persistentDelivery The persistentDelivery to set. */
    public void setPersistentDelivery(boolean persistentDelivery)
    {
        this.persistentDelivery = persistentDelivery;
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

    public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public int getMaxRedelivery()
    {
        return maxRedelivery;
    }

    public void setMaxRedelivery(int maxRedelivery)
    {
        this.maxRedelivery = maxRedelivery;
    }

    public boolean isResponseEnabled()
    {
        return true;
    }


    /**
     * Getter for property 'topicResolver'.
     *
     * @return Value for property 'topicResolver'.
     */
    public JmsTopicResolver getTopicResolver()
    {
        return topicResolver;
    }

    /**
     * Setter for property 'topicResolver'.
     *
     * @param topicResolver Value to set for property 'topicResolver'.
     */
    public void setTopicResolver(final JmsTopicResolver topicResolver)
    {
        this.topicResolver = topicResolver;
    }

    /**
     * Getter for property 'eagerConsumer'. Default
     * is {@code true}.
     *
     * @return Value for property 'eagerConsumer'.
     * @see #eagerConsumer
     */
    public boolean isEagerConsumer()
    {
        return eagerConsumer;
    }

    /**
     * A value of {@code true} will create a consumer on
     * connect, in contrast to lazy instantiation in the poll loop.
     * This setting very much depends on the JMS vendor.
     * Affects transactional receivers, typical symptoms are:
     * <ul>
     * <li> consumer thread hanging forever, though a message is
     * available
     * <li>failure to consume the first message (the rest
     * are fine)
     * </ul>
     * <p/>
     *
     * @param eagerConsumer Value to set for property 'eagerConsumer'.
     * @see #eagerConsumer
     * @see org.mule.transport.jms.XaTransactedJmsMessageReceiver
     */
    public void setEagerConsumer(final boolean eagerConsumer)
    {
        this.eagerConsumer = eagerConsumer;
    }

    public boolean isCacheJmsSessions()
    {
        return cacheJmsSessions;
    }

    public void setCacheJmsSessions(boolean cacheJmsSessions)
    {
        this.cacheJmsSessions = cacheJmsSessions;
    }

    public ConnectionFactory getConnectionFactory()
    {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory)
    {
        this.connectionFactory = connectionFactory;
    }
    
    public RedeliveryHandlerFactory getRedeliveryHandlerFactory()
    {
        return redeliveryHandlerFactory;
    }
    
    public void setRedeliveryHandlerFactory(RedeliveryHandlerFactory redeliveryHandlerFactory)
    {
        this.redeliveryHandlerFactory = redeliveryHandlerFactory;
    }

    /**
     * Sets the <code>honorQosHeaders</code> property, which determines whether
     * {@link JmsMessageDispatcher} should honor incoming message's QoS headers
     * (JMSPriority, JMSDeliveryMode).
     * 
     * @param honorQosHeaders <code>true</code> if {@link JmsMessageDispatcher}
     *            should honor incoming message's QoS headers; otherwise
     *            <code>false</code> Default is <code>false</code>, meaning that
     *            connector settings will override message headers.
     */
   public void setHonorQosHeaders(boolean honorQosHeaders)
   {
       this.honorQosHeaders = honorQosHeaders;
   }

   /**
     * Gets the value of <code>honorQosHeaders</code> property.
     * 
     * @return <code>true</code> if <code>JmsMessageDispatcher</code> should
     *         honor incoming message's QoS headers; otherwise <code>false</code>
     *         Default is <code>false</code>, meaning that connector settings will
     *         override message headers.
     */
   public boolean isHonorQosHeaders()
   {
       return honorQosHeaders;
   }

   public Context getJndiContext()
   {
       return jndiContext;
   }

   public void setJndiContext(Context jndiContext)
   {
       this.jndiContext = jndiContext;
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

   public Map getJndiProviderProperties()
   {
       return jndiProviderProperties;
   }

   public void setJndiProviderProperties(Map jndiProviderProperties)
   {
       this.jndiProviderProperties = jndiProviderProperties;
   }

   public String getConnectionFactoryJndiName()
   {
       return connectionFactoryJndiName;
   }

   public void setConnectionFactoryJndiName(String connectionFactoryJndiName)
   {
       this.connectionFactoryJndiName = connectionFactoryJndiName;
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

    public boolean isDisableTemporaryReplyToDestinations()
    {
        return disableTemporaryReplyToDestinations;
    }

    public void setDisableTemporaryReplyToDestinations(boolean disableTemporaryReplyToDestinations)
    {
        this.disableTemporaryReplyToDestinations = disableTemporaryReplyToDestinations;
    }

   /**
    * @return Returns underlying connection factory properties.
    */
   public Map getConnectionFactoryProperties()
   {
       return connectionFactoryProperties;
   }

   /**
    * @param connectionFactoryProperties properties to be set on the underlying
    *            ConnectionFactory.
    */
   public void setConnectionFactoryProperties(Map connectionFactoryProperties)
   {
       this.connectionFactoryProperties = connectionFactoryProperties;
   }

    /**
     * A synonym for {@link #numberOfConcurrentTransactedReceivers}. Note that
     * it affects both transactional and non-transactional scenarios.
     * @param count number of consumers
     */
    public void setNumberOfConsumers(int count)
    {
        this.numberOfConcurrentTransactedReceivers = count;
    }

    /**
     * A synonym for {@link #numberOfConcurrentTransactedReceivers}.
     * @return number of consumers
     */
    public int getNumberOfConsumers()
    {
        return this.numberOfConcurrentTransactedReceivers;
    }

    public boolean isEmbeddedMode()
    {
        return embeddedMode;
    }

    public void setEmbeddedMode(boolean embeddedMode)
    {
        this.embeddedMode = embeddedMode;
    }
}
