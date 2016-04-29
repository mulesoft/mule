/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.Closeable;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.BeanUtils;
import org.mule.runtime.transport.jms.filters.JmsSelectorFilter;
import org.mule.runtime.transport.jms.i18n.JmsMessages;
import org.mule.runtime.transport.jms.jndi.JndiNameResolver;
import org.mule.runtime.transport.jms.jndi.SimpleJndiNameResolver;
import org.mule.runtime.transport.jms.redelivery.AutoDiscoveryRedeliveryHandlerFactory;
import org.mule.runtime.transport.jms.redelivery.RedeliveryHandlerFactory;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.naming.CommunicationException;
import javax.naming.NamingException;

/**
 * <code>JmsConnector</code> is a JMS 1.0.2b compliant connector that can be used
 * by a Mule endpoint. The connector supports all JMS functionality including topics
 * and queues, durable subscribers, acknowledgement modes and local transactions.
 *
 * From 3.6, JMS Sessions and Producers are reused by default when an {@link javax.jms.XAConnectionFactory} isn't being
 * used and when the (default) JMS 1.1 spec is being used.
 */
public class JmsConnector extends AbstractConnector implements ExceptionListener
{

    public static final String JMS = "jms";

    /**
     * Indicates that Mule should throw an exception on any redelivery attempt.
     */
    public static final int REDELIVERY_FAIL_ON_FIRST = 0;

    public static final int REDELIVERY_IGNORE = -1;

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

    private int maxRedelivery = REDELIVERY_FAIL_ON_FIRST;

    private boolean cacheJmsSessions = true;

    /**
     * Whether to create a consumer on connect.
     */
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
    private String jndiProviderUrl;

    private String jndiInitialFactory;

    private Map jndiProviderProperties;

    private String connectionFactoryJndiName;

    private boolean jndiDestinations = false;

    private boolean forceJndiDestinations = false;

    /**
     * Resolves JNDI names if the connector uses {@link #jndiDestinations}
     */
    private JndiNameResolver jndiNameResolver;

    ////////////////////////////////////////////////////////////////////////
    // Strategy classes
    ////////////////////////////////////////////////////////////////////////

    private String specification = JmsConstants.JMS_SPECIFICATION_102B;

    private JmsSupport jmsSupport;

    private JmsTopicResolver topicResolver;

    private RedeliveryHandlerFactory redeliveryHandlerFactory;

    /**
     * determines whether a temporary JMSReplyTo destination will be used when using synchronous outbound JMS endpoints
     */
    private boolean disableTemporaryReplyToDestinations = false;

    /**
     * If disableTemporaryReplyToDestinations = "true", this flag causes the original JMS Message to be returned as a
     * synchronous response with any properties set on it by the JMS Provider (e.g., JMSMessageID).
     *
     * @see EE-1688/MULE-3059
     */
    private boolean returnOriginalMessageAsReply = false;

    /**
     * In-container embedded mode disables some features for strict Java EE compliance.
     */
    private boolean embeddedMode;

    /**
     * Overrides XaResource.isSameRM() result. Needed for IBM WMQ XA
     * implementation (set to 'false'). Default value is null (don't override).
     */
    private Boolean sameRMOverrideValue;

    private final CompositeConnectionFactoryDecorator connectionFactoryDecorator = new CompositeConnectionFactoryDecorator();

    /**
     * Used to ignore handling of ExceptionListener#onException when in the process of disconnecting.  This is
     * required because the Connector {@link org.mule.api.lifecycle.LifecycleManager} does not include
     * connection/disconnection state.
     */
    private volatile boolean disconnecting;

    ////////////////////////////////////////////////////////////////////////
    // Methods
    ////////////////////////////////////////////////////////////////////////

    /* Register the Jms Exception reader if this class gets loaded */

    static
    {
        ExceptionHelper.registerExceptionReader(new JmsExceptionReader());
    }

    public JmsConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    public String getProtocol()
    {
        return JMS;
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (jmsSupport == null)
        {
            jmsSupport = createJmsSupport();
        }
        connectionFactoryDecorator.init(muleContext);
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
            muleContext.registerListener(new ConnectionNotificationListener<ConnectionNotification>()
            {
                @Override
                public void onNotification(ConnectionNotification notification)
                {
                    if (notification.getAction() == ConnectionNotification.CONNECTION_DISCONNECTED
                        || notification.getAction() == ConnectionNotification.CONNECTION_FAILED)
                    {
                        // Remove all dispatchers as any cached session will be invalidated
                        clearDispatchers();
                        // TODO should we dispose receivers here as well (in case they are
                        // transactional)
                        // gives a harmless NPE at
                        // AbstractConnector.connect(AbstractConnector.java:927)
                        // disposeReceivers();
                    }
                }

            }, getName());


        }
        catch (NotificationException nex)
        {
            throw new InitialisationException(nex, this);
        }

    }

    /**
     * A factory method to create various JmsSupport class versions.
     *
     * @see JmsSupport
     * @return JmsSupport instance
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

    protected ConnectionFactory createConnectionFactory() throws NamingException, MuleException
    {
        // if an initial factory class was configured that takes precedence over the 
        // spring-configured connection factory or the one that our subclasses may provide
        if (jndiInitialFactory != null || jndiNameResolver != null)
        {
            if (jndiNameResolver == null)
            {
                jndiNameResolver = createDefaultJndiResolver();
            }
            jndiNameResolver.initialise();

            Object temp = jndiNameResolver.lookup(connectionFactoryJndiName);
            if (temp instanceof ConnectionFactory)
            {
                return (ConnectionFactory) temp;
            }
            else
            {
                throw new DefaultMuleException(
                        JmsMessages.invalidResourceType(ConnectionFactory.class, temp));
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
            ConnectionFactory factory;
            try
            {
                factory = getDefaultConnectionFactory();
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
            if (factory == null)
            {
                // no connection factory ... give up
                throw new DefaultMuleException(JmsMessages.noConnectionFactoryConfigured());
            }
            return factory;
        }
    }

    private JndiNameResolver createDefaultJndiResolver()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Creating default JndiNameResolver");
        }

        SimpleJndiNameResolver jndiContextFactory = new SimpleJndiNameResolver();
        jndiContextFactory.setJndiProviderUrl(jndiProviderUrl);
        jndiContextFactory.setJndiInitialFactory(jndiInitialFactory);
        jndiContextFactory.setJndiProviderProperties(jndiProviderProperties);

        return jndiContextFactory;
    }

    /**
     * Override this method to provide a default ConnectionFactory for a vendor-specific JMS Connector.
     * @throws Exception
     */
    protected ConnectionFactory getDefaultConnectionFactory() throws Exception
    {
        return null;
    }

    @Override
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

        if (connectionFactory instanceof Disposable)
        {
            ((Disposable) connectionFactory).dispose();
        }

        if (jndiNameResolver != null)
        {
            jndiNameResolver.dispose();
        }
    }

    protected Object lookupFromJndi(String jndiName) throws NamingException
    {
        try
        {
            return jndiNameResolver.lookup(jndiName);
        }
        catch (CommunicationException ce)
        {
            try
            {
                final Transaction tx = TransactionCoordination.getInstance().getTransaction();
                if (tx != null)
                {
                    tx.setRollbackOnly();
                }
            }
            catch (TransactionException e)
            {
                throw new MuleRuntimeException(
                        MessageFactory.createStaticMessage("Failed to mark transaction for rollback: "), e);
            }

            // re-throw
            throw ce;
        }
    }

    protected Connection createConnection() throws MuleException, JMSException
    {
        if (connectionFactory == null)
        {
            try
            {
                connectionFactory = this.createConnectionFactory();
            }
            catch (NamingException ne)
            {
                throw new DefaultMuleException(JmsMessages.errorCreatingConnectionFactory(), ne);
            }
        }
        if ((connectionFactoryProperties != null) && !connectionFactoryProperties.isEmpty())
        {
            // apply connection factory properties
            BeanUtils.populateWithoutFail(connectionFactory, connectionFactoryProperties, true);
        }

        connectionFactory = connectionFactoryDecorator.decorate(connectionFactory, this, muleContext);

        Connection connection;

        if (!cacheJmsSessions && username != null)
        {
            connection = jmsSupport.createConnection(connectionFactory, username, password);
        }
        else
        {
            connection = jmsSupport.createConnection(connectionFactory);
        }

        if (connection != null)
        {
            // EE-1901: only sets the clientID if it was not already set
            if (clientId != null && !clientId.equals(connection.getClientID()))
            {
                connection.setClientID(getClientId());
            }
            // Only set the exceptionListener if one isn't already set.  This is because the CachingConnectionFactory
            // which may be in use doesn't permit exception strategy to be set, rather it is set on the ConnectionFactory
            // itself in this case.
            if (!embeddedMode && connection.getExceptionListener() == null)
            {
                connection.setExceptionListener(this);
            }
        }
        return connection;
    }

    @Override
    public void connect() throws Exception {
        if (muleContext.isPrimaryPollingInstance() || clientId == null)
        {
            super.connect();
        }
        else
        {
            muleContext.registerListener(new ClusterNodeNotificationListener<ClusterNodeNotification>()
            {
                @Override
                public void onNotification(ClusterNodeNotification notification)
                {
                    // Notification thread is bound to the MuleContainerSystemClassLoader, save it 
                    // so we can restore it later
                    ClassLoader notificationClassLoader = Thread.currentThread().getContextClassLoader();
                    try
                    {
                        // The connection should use instead the ApplicationClassloader
                        Thread.currentThread().setContextClassLoader(muleContext.getExecutionClassLoader());
                        
                        JmsConnector.this.connect();
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(e);
                    }
                    finally 
                    {
                        // Restore the notification original class loader so we don't interfere in any later
                        // usage of this thread
                        Thread.currentThread().setContextClassLoader(notificationClassLoader);
                    }
                }
            });
        }
    }

    @Override
    public void onException(JMSException jmsException)
    {
        if (!disconnecting)
        {
            Map<Object, MessageReceiver> receivers = getReceivers();
            boolean isMultiConsumerReceiver = false;

            if (!receivers.isEmpty())
            {
                MessageReceiver reciever = receivers.values().iterator().next();
                if (reciever instanceof MultiConsumerJmsMessageReceiver)
                {
                    isMultiConsumerReceiver = true;
                }
            }

            int expectedReceiverCount = isMultiConsumerReceiver ? 1 :
                                        (getReceivers().size() * getNumberOfConcurrentTransactedReceivers());

            if (logger.isDebugEnabled())
            {
                logger.debug("About to recycle myself due to remote JMS connection shutdown but need "
                             + "to wait for all active receivers to report connection loss. Receiver count: "
                             + (receiverReportedExceptionCount.get() + 1) + '/' + expectedReceiverCount);
            }

            if (receiverReportedExceptionCount.incrementAndGet() >= expectedReceiverCount)
            {
                receiverReportedExceptionCount.set(0);
                muleContext.getExceptionListener().handleException(new ConnectException(jmsException, this));
            }
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        connection = createConnection();
        if ((connectionFactoryProperties != null) && !connectionFactoryProperties.isEmpty())
        {
            // apply connection factory properties
            BeanUtils.populateWithoutFail(connectionFactory, connectionFactoryProperties, true);
        }
        if (isStarted() || startOnConnect)
        {
            try
            {
                connection.start();
            }
            catch (Exception e)
            {
                // If connection throws an exception on start and connection is cached in ConnectionFactory then
                // close/reset connection now.
                if (connectionFactory instanceof Closeable)
                {
                    ((Closeable) connectionFactory).close();
                }
                throw e;
            }
        }
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        try
        {
            if (connection != null)
            {
                disconnecting = true;
                connection.close();
                if (connectionFactory instanceof Closeable)
                {
                    ((Closeable) connectionFactory).close();
                }
            }
        }
        finally
        {
            connection = null;
            disconnecting = false;
        }
    }

    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        return flowConstruct.getName() + "~" + endpoint.getEndpointURI().getAddress();
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

    public Session createSession(ImmutableEndpoint endpoint) throws JMSException
    {
        return createSession(endpoint.getTransactionConfig().isTransacted(),getTopicResolver().isTopic(endpoint));
    }

    public Session getSession(boolean transacted, boolean topic) throws JMSException
    {
        Session session = getSessionFromTransaction();
        if (session != null)
        {
            return session;
        }

        Transaction tx = TransactionCoordination.getInstance().getTransaction();

        session = createSession(transacted, topic);

        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format(
                    "Retrieved new jms session from connection: " +
                    "topic={0}, transacted={1}, ack mode={2}, nolocal={3}: {4}",
                    topic, transacted, acknowledgementMode, noLocal, session));
        }

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

    private Session createSession(boolean transacted, boolean topic) throws JMSException
    {
        Session session;
        session = jmsSupport.createSession(connection, topic, transacted, acknowledgementMode, noLocal);
        return session;
    }

    @Override
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

        if (jndiNameResolver != null)
        {
            jndiNameResolver.start();
        }
    }


    /**
     * Closes a session if there is no active transaction in the current thread, otherwise the
     * session will continue active until there is a direct call to close it.
     *
     * @param session the session that ill be closed if there is an active transaction.
     */
    public void closeSessionIfNoTransactionActive(Session session)
    {
        final Transaction transaction = TransactionCoordination.getInstance().getTransaction();
        if (transaction == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.error("Closing non-TX session: " + session);
            }
            closeQuietly(session);
        }
        else if (logger.isDebugEnabled())
        {
            logger.error("Not closing TX session: " + session);
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        if (connection != null)
        {
            try
            {
                connection.stop();
            }
            catch (Exception e)
            {
                // this exception may be thrown when the broker is shut down, but the
                // stop process should continue all the same
                logger.warn("Jms connection failed to stop properly: ", e);
            }
        }

        if (jndiNameResolver != null)
        {
            jndiNameResolver.stop();
        }
    }

    @Override
    public ReplyToHandler getReplyToHandler(ImmutableEndpoint endpoint)
    {
        return new JmsReplyToHandler(this, endpoint.getMuleContext());
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
            if (logger.isDebugEnabled())
            {
                logger.debug("Closing producer: " + producer);
            }
            producer.close();
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Producer is null, nothing to close");
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
        catch (Exception e)
        {
            logger.warn("Failed to close jms message producer: " + e.getMessage());
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
            if (logger.isDebugEnabled())
            {
                logger.debug("Closing consumer: " + consumer);
            }
            consumer.close();
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Consumer is null, nothing to close");
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
        catch (Exception e)
        {
            logger.warn("Failed to close jms message consumer: " + e.getMessage());
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
        if (session != null)
        {
            try
            {
                close(session);
            }
            catch (Exception e)
            {
                logger.warn("Failed to close jms session consumer: " + e.getMessage());
            }
            finally
            {
                session = null;
            }
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
        catch (Exception e)
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
        catch (Exception e)
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
                logger.warn("Failed to delete a temporary topic " + topicName + ": " + e.getMessage());
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ////////////////////////////////////////////////////////////////////////

    /**
     * @return Returns the connection.
     */
    public Connection getConnection()
    {
        return connection;
    }

    protected void setConnection(Connection connection)
    {
        this.connection = connection;
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
        if (JmsConstants.JMS_SPECIFICATION_11.equals(specification)
            || (JmsConstants.JMS_SPECIFICATION_102B.equals(specification)))
        {
            this.specification = specification;
        }
        else
        {
            throw new IllegalArgumentException(
                    "JMS specification needs to be one of the defined values in JmsConstants but was: "
                    + specification);
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

    @Override
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
     * @see org.mule.runtime.transport.jms.XaTransactedJmsMessageReceiver
     */
    public void setEagerConsumer(final boolean eagerConsumer)
    {
        this.eagerConsumer = eagerConsumer;
    }

    /**
     * @return true if session caching is enabled
     */
    public boolean isCacheJmsSessions()
    {
        return cacheJmsSessions;
    }

    /**
     * @param cacheJmsSessions true if session should be enabled
     */
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
     *                        should honor incoming message's QoS headers; otherwise
     *                        <code>false</code> Default is <code>false</code>, meaning that
     *                        connector settings will override message headers.
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

    /**
     * @deprecated use a {@link JndiNameResolver} instead of access this property
     */
    @Deprecated
    public String getJndiInitialFactory()
    {
        return jndiInitialFactory;
    }

    /**
     * @deprecated use a {@link JndiNameResolver} instead of access this property
     */
    @Deprecated
    public void setJndiInitialFactory(String jndiInitialFactory)
    {
        this.jndiInitialFactory = jndiInitialFactory;
    }

    /**
     * @deprecated use a {@link JndiNameResolver} instead of access this property
     */
    @Deprecated
    public String getJndiProviderUrl()
    {
        return jndiProviderUrl;
    }

    /**
     * @deprecated use a {@link JndiNameResolver} instead of access this property
     */
    @Deprecated
    public void setJndiProviderUrl(String jndiProviderUrl)
    {
        this.jndiProviderUrl = jndiProviderUrl;
    }

    /**
     * @deprecated use a {@link JndiNameResolver} instead of access this property
     */
    @Deprecated
    public Map getJndiProviderProperties()
    {
        return jndiProviderProperties;
    }

    /**
     * @deprecated use a {@link JndiNameResolver} instead of access this property
     */
    @Deprecated
    public void setJndiProviderProperties(Map jndiProviderProperties)
    {
        this.jndiProviderProperties = jndiProviderProperties;
    }

    public JndiNameResolver getJndiNameResolver()
    {
        return jndiNameResolver;
    }

    public void setJndiNameResolver(JndiNameResolver jndiNameResolver)
    {
        this.jndiNameResolver = jndiNameResolver;
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

    public boolean isReturnOriginalMessageAsReply()
    {
        return returnOriginalMessageAsReply;
    }

    public void setReturnOriginalMessageAsReply(boolean returnOriginalMessageAsReply)
    {
        this.returnOriginalMessageAsReply = returnOriginalMessageAsReply;
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
     *                                    ConnectionFactory.
     */
    public void setConnectionFactoryProperties(Map connectionFactoryProperties)
    {
        this.connectionFactoryProperties = connectionFactoryProperties;
    }

    /**
     * A synonym for {@link #numberOfConcurrentTransactedReceivers}. Note that
     * it affects both transactional and non-transactional scenarios.
     *
     * @param count number of consumers
     */
    public void setNumberOfConsumers(int count)
    {
        this.numberOfConcurrentTransactedReceivers = count;
    }

    /**
     * A synonym for {@link #numberOfConcurrentTransactedReceivers}.
     *
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

    public Boolean getSameRMOverrideValue()
    {
        return sameRMOverrideValue;
    }

    public void setSameRMOverrideValue(Boolean sameRMOverrideValue)
    {
        this.sameRMOverrideValue = sameRMOverrideValue;
    }

    public JmsSelectorFilter getSelector(ImmutableEndpoint endpoint)
    {
        for (MessageProcessor mp : endpoint.getMessageProcessors())
        {
            if (mp instanceof JmsSelectorFilter)
            {
                return (JmsSelectorFilter) mp;
            }
            else if (mp instanceof MessageFilter)
            {
                MessageFilter mf = (MessageFilter) mp;
                if (mf.getFilter() instanceof JmsSelectorFilter)
                {
                    return (JmsSelectorFilter) mf.getFilter();
                }
            }
        }

        return null;
    }

    @Override
    protected Session createOperationResource(ImmutableEndpoint endpoint) throws MuleException
    {
        try
        {
            return createSession(endpoint);
        }
        catch (JMSException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    protected Object getOperationResourceFactory()
    {
        return getConnection();
    }
}
