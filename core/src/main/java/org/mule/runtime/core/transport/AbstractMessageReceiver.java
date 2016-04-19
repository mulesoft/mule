/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.ResponseOutputStream;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.MessageProcessTemplate;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate;
import org.mule.runtime.core.lifecycle.PrimaryNodeLifecycleNotificationListener;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.work.TrackingWorkManager;
import org.mule.runtime.core.work.WorkManagerHolder;

import java.io.OutputStream;
import java.util.List;


/**
 * <code>AbstractMessageReceiver</code> provides common methods for all Message
 * Receivers provided with Mule. A message receiver enables an endpoint to receive a
 * message from an external system.
 */
public abstract class AbstractMessageReceiver extends AbstractTransportMessageHandler implements MessageReceiver
{
    /**
     * The Service with which this receiver is associated with
     */
    protected FlowConstruct flowConstruct;

    /**
     * {@link MessageProcessor} chain used to process messages once the transport
     * specific {@link MessageReceiver} has received transport message and created
     * the {@link MuleEvent}
     */
    protected MessageProcessor listener;

    /**
     * Stores the key to this receiver, as used by the Connector to store the
     * receiver.
     */
    protected String receiverKey = null;

    /**
     * Stores the endpointUri that this receiver listens on. This enpoint can be
     * different to the endpointUri in the endpoint stored on the receiver as
     * endpoint endpointUri may get rewritten if this endpointUri is a wildcard
     * endpointUri such as jms.*
     */
    private EndpointURI endpointUri;

    protected List<Transformer> defaultInboundTransformers;
    protected List<Transformer> defaultResponseTransformers;

    protected ReplyToHandler replyToHandler;
    private PrimaryNodeLifecycleNotificationListener primaryNodeLifecycleNotificationListener;
    private MessageProcessingManager messageProcessingManager;

    private WorkManager messageReceiverWorkManager;

    /**
     * Creates the Message Receiver
     *
     * @param connector the endpoint that created this listener
     * @param flowConstruct the flow construct to associate with the receiver.
     * @param endpoint the provider contains the endpointUri on which the receiver
     *            will listen on. The endpointUri can be anything and is specific to
     *            the receiver implementation i.e. an email address, a directory, a
     *            jms destination or port address.
     * @see FlowConstruct
     * @see InboundEndpoint
     */
    public AbstractMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint)
        throws CreateException
    {
        super(endpoint);

        if (flowConstruct == null)
        {
            throw new IllegalArgumentException("FlowConstruct cannot be null");
        }
        this.flowConstruct = flowConstruct;

        messageReceiverWorkManager = createWorkManager();
    }

    @Override
    protected ConnectableLifecycleManager createLifecycleManager()
    {
        return new ConnectableLifecycleManager<MessageReceiver>(getReceiverKey(), this);
    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during
     * initialisation an <code>InitialisationException</code> should be thrown,
     * causing the Mule instance to shutdown. If the error is recoverable, say by
     * retrying to connect, a <code>RecoverableException</code> should be thrown.
     * There is no guarantee that by throwing a Recoverable exception that the Mule
     * instance will not shut down.
     *
     * @throws org.mule.api.lifecycle.InitialisationException if a fatal error occurs
     *             causing the Mule instance to shutdown
     * @throws org.mule.api.lifecycle.RecoverableException if an error occurs that
     *             can be recovered from
     */
    @Override
    public final void initialise() throws InitialisationException
    {
        endpointUri = endpoint.getEndpointURI();

        defaultInboundTransformers = connector.getDefaultInboundTransformers(endpoint);
        defaultResponseTransformers = connector.getDefaultResponseTransformers(endpoint);

        replyToHandler = getReplyToHandler();

        if (!shouldConsumeInEveryNode() && !flowConstruct.getMuleContext().isPrimaryPollingInstance())
        {
            primaryNodeLifecycleNotificationListener = new PrimaryNodeLifecycleNotificationListener(new Startable()
            {
                @Override
                public void start() throws MuleException
                {
                    if (AbstractMessageReceiver.this.isStarted())
                    {
                        try
                        {
                            AbstractMessageReceiver.this.doConnect();
                        }
                        catch (Exception e)
                        {
                            throw new DefaultMuleException(e);
                        }
                        AbstractMessageReceiver.this.doStart();
                    }
                }
            },flowConstruct.getMuleContext());
            primaryNodeLifecycleNotificationListener.register();
        }

        messageProcessingManager = getEndpoint().getMuleContext().getRegistry().get(MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER);

        super.initialise();
    }

    @Override
    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    @Override
    public final MuleEvent routeMessage(MuleMessage message) throws MuleException
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, null);
    }

    @Override
    public final MuleEvent routeMessage(MuleMessage message, Transaction trans) throws MuleException
    {
        return routeMessage(message, trans, null);
    }

    @Override
    public final MuleEvent routeMessage(MuleMessage message, Transaction trans, OutputStream outputStream)
            throws MuleException
    {
        return routeMessage(message, new DefaultMuleSession(), trans, outputStream);
    }

    public final MuleEvent routeMessage(MuleMessage message,
                                        MuleSession session,
                                        Transaction trans,
                                        OutputStream outputStream) throws MuleException
    {
        return routeMessage(message, session, outputStream);
    }

    public final MuleEvent routeMessage(MuleMessage message, MuleSession session, OutputStream outputStream)
        throws MuleException
    {

        warnIfMuleClientSendUsed(message);

        propagateRootMessageIdProperty(message);

        MuleEvent muleEvent = createMuleEvent(message, outputStream);

        if (!endpoint.isDisableTransportTransformer())
        {
            applyInboundTransformers(muleEvent);
        }

        return routeEvent(muleEvent);
    }

    protected void propagateRootMessageIdProperty(MuleMessage message)
    {
        String rootId = message.getInboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY);
        if (rootId != null)
        {
            message.setMessageRootId(rootId);
            message.removeProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY, PropertyScope.INBOUND);
        }
    }

    protected void warnIfMuleClientSendUsed(MuleMessage message)
    {
        final Object remoteSyncProperty = message.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY,
            PropertyScope.INBOUND);
        if (ObjectUtils.getBoolean(remoteSyncProperty, false) && !endpoint.getExchangePattern().hasResponse())
        {
            logger.warn("MuleClient.send() was used but inbound endpoint "
                        + endpoint.getEndpointURI().getUri().toString()
                        + " is not 'request-response'.  No response will be returned.");
        }

        message.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, PropertyScope.INBOUND);
    }

    protected void applyInboundTransformers(MuleEvent event) throws MuleException
    {
        event.setMessage(getTransformationService().applyTransformers(event.getMessage(), event, defaultInboundTransformers));

    }

    protected void applyResponseTransformers(MuleEvent event) throws MuleException
    {
        event.setMessage(getTransformationService().applyTransformers(event.getMessage(), event, defaultResponseTransformers));
    }

    protected MuleMessage handleUnacceptedFilter(MuleMessage message)
    {
        if (logger.isDebugEnabled())
        {
            String messageId;
            messageId = message.getUniqueId();
            logger.debug("Message " + messageId + " failed to pass filter on endpoint: " + endpoint
                         + ". Message is being ignored");
        }
        return message;
    }

    protected MuleEvent createMuleEvent(MuleMessage message, OutputStream outputStream)
        throws MuleException
    {
        MuleEvent event;
        ResponseOutputStream ros = null;
        if (outputStream != null)
        {
            if (outputStream instanceof ResponseOutputStream)
            {
                ros = (ResponseOutputStream) outputStream;
            }
            else
            {
                ros = new ResponseOutputStream(outputStream);
            }
        }
        MuleSession session = connector.getSessionHandler().retrieveSessionInfoFromMessage(message);

        if (session == null)
        {
            session = new DefaultMuleSession();
        }
        if (message.getReplyTo() != null)
        {
            DefaultMuleEvent newEvent = new DefaultMuleEvent(message, flowConstruct, session, replyToHandler, message.getReplyTo(), ros);
            newEvent.populateFieldsFromInboundEndpoint(getEndpoint());
            event = newEvent;
            message.setReplyTo(null);
        }
        else
        {
            DefaultMuleEvent newEvent = new DefaultMuleEvent(message, flowConstruct, session, null, null, ros);
            newEvent.populateFieldsFromInboundEndpoint(getEndpoint());
            event = newEvent;
        }
        event = OptimizedRequestContext.unsafeSetEvent(event);
        if (session.getSecurityContext() != null && session.getSecurityContext().getAuthentication() != null)
        {
            session.getSecurityContext().getAuthentication().setEvent(event);
        }
        return event;
    }

    @Override
    public EndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    @Override
    public String getConnectionDescription()
    {
        return endpoint.getEndpointURI().toString();
    }

    protected String getConnectEventId()
    {
        return connector.getName() + ".receiver (" + endpoint.getEndpointURI() + ")";
    }

    // TODO MULE-4871 Receiver key should not be mutable
    @Override
    public void setReceiverKey(String receiverKey)
    {
        this.receiverKey = receiverKey;
    }

    @Override
    public String getReceiverKey()
    {
        return receiverKey;
    }

    @Override
    public InboundEndpoint getEndpoint()
    {
        return (InboundEndpoint) super.getEndpoint();
    }

    // TODO MULE-4871 Endpoint should not be mutable
    @Override
    public void setEndpoint(InboundEndpoint endpoint)
    {
        super.setEndpoint(endpoint);
    }

    @Override
    protected WorkManager getWorkManager()
    {
        return messageReceiverWorkManager;
    }

    private WorkManager getConnectorWorkManager()
    {
        try
        {
            return connector.getReceiverWorkManager();
        }
        catch (MuleException e)
        {
            logger.error(e);
            return null;
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", receiverKey=").append(receiverKey);
        sb.append(", endpoint=").append(endpoint.getEndpointURI());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void setListener(MessageProcessor processor)
    {
        this.listener = processor;
    }

    @Override
    protected void doDispose()
    {
        this.listener = null;
        this.flowConstruct = null;
        if (primaryNodeLifecycleNotificationListener != null)
        {
            primaryNodeLifecycleNotificationListener.unregister();
        }
        super.doDispose();
    }

    protected ReplyToHandler getReplyToHandler()
    {
        return ((AbstractConnector) endpoint.getConnector()).getReplyToHandler(endpoint);
    }

    protected ExecutionTemplate<MuleEvent> createExecutionTemplate()
    {
        return TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate(endpoint.getMuleContext(), endpoint.getTransactionConfig(),
                                                                                       flowConstruct.getExceptionListener());
    }

    /**
     * Determines whether to start or not the MessageSource base on the running node state.
     *
     * @return false if this MessageSource should be stated only in the primary node, true if it should be started in every node.
     */
    public boolean shouldConsumeInEveryNode()
    {
        return true;
    }

    @Override
    final protected void connectHandler() throws Exception
    {
        if (shouldConsumeInEveryNode() || getFlowConstruct().getMuleContext().isPrimaryPollingInstance())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Connecting clusterizable message receiver");
            }

            doConnect();
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Clusterizable message receiver not connected on this node");
            }
        }
    }

    @Override
    final protected void doStartHandler() throws MuleException
    {
        if (shouldConsumeInEveryNode() || getFlowConstruct().getMuleContext().isPrimaryPollingInstance())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Starting clusterizable message receiver");
            }
            if (messageReceiverWorkManager == null)
            {
                messageReceiverWorkManager = createWorkManager();
            }

            doStart();
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Clusterizable message receiver not started on this node");
            }
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();

        if (messageReceiverWorkManager != null)
        {
            messageReceiverWorkManager.dispose();
            messageReceiverWorkManager = null;
        }
    }

    private WorkManager createWorkManager()
    {
        int shutdownTimeout = endpoint.getMuleContext().getConfiguration().getShutdownTimeout();

        return new TrackingWorkManager(new WorkManagerHolder()
        {
            @Override
            public WorkManager getWorkManager()
            {
                return getConnectorWorkManager();
            }
        }, shutdownTimeout);
    }

    public MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException
    {
        MuleEvent resultEvent = listener.process(muleEvent);
        if (resultEvent != null
            && !VoidMuleEvent.getInstance().equals(resultEvent)
            && resultEvent.getMessage() != null
            && resultEvent.getMessage().getExceptionPayload() != null
            && resultEvent.getMessage().getExceptionPayload().getException() instanceof FilterUnacceptedException)
        {
            handleUnacceptedFilter(muleEvent.getMessage());
            return muleEvent;
        }

        if (endpoint.getExchangePattern().hasResponse() && resultEvent != null
            && !VoidMuleEvent.getInstance().equals(resultEvent))
        {
            // Do not propagate security context back to caller
            MuleSession resultSession = new DefaultMuleSession(resultEvent.getSession());
            resultSession.setSecurityContext(null);
            connector.getSessionHandler().storeSessionInfoToMessage(resultSession, resultEvent.getMessage());

            if (resultEvent.getMessage() != null && !endpoint.isDisableTransportTransformer())
            {
                applyResponseTransformers(resultEvent);
            }

            if (connector.isEnableMessageEvents(muleEvent))
            {
                connector.fireNotification(new EndpointMessageNotification(resultEvent.getMessage(),
                                                                           endpoint,
                                                                           resultEvent.getFlowConstruct(),
                                                                           EndpointMessageNotification.MESSAGE_RESPONSE),
                                           muleEvent);
            }
        }
        return resultEvent;
    }

    protected void processMessage(final MessageProcessTemplate messageProcessTemplate, final MessageProcessContext messageProcessContext)
    {
        messageProcessingManager.processMessage(messageProcessTemplate,messageProcessContext);
    }

}
