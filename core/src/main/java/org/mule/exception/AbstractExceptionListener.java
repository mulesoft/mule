/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.exception;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.service.Service;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.DispatchException;
import org.mule.api.util.StreamCloserService;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ExceptionNotification;
import org.mule.message.ExceptionMessage;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.session.DefaultMuleSession;
import org.mule.transaction.TransactionCoordination;
import org.mule.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractExceptionListener</code> is a base implementation that custom
 * Exception Listeners can override. It provides template methods for handling the
 * for base types of exceptions plus allows multimple targets to be associated with
 * this exception listener and provides an implementation for dispatching exception
 * events from this Listener.
 */
public abstract class AbstractExceptionListener extends AbstractMessageProcessorOwner
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    @SuppressWarnings("unchecked")
    protected List<MessageProcessor> messageProcessors = new CopyOnWriteArrayList();

    protected AtomicBoolean initialised = new AtomicBoolean(false);

    protected WildcardFilter rollbackTxFilter;
    protected WildcardFilter commitTxFilter;

    protected boolean enableNotifications = true;

    public List<MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }

    public void setMessageProcessors(List<MessageProcessor> processors)
    {
        if (processors != null)
        {
            this.messageProcessors.clear();
            this.messageProcessors.addAll(processors);
        }
        else
        {
            throw new IllegalArgumentException("List of targets = null");
        }
    }

    public void addEndpoint(MessageProcessor processor)
    {
        if (processor != null)
        {
            messageProcessors.add(processor);
        }
    }

    public boolean removeMessageProcessor(MessageProcessor processor)
    {
        return messageProcessors.remove(processor);
    }

    protected Throwable getExceptionType(Throwable t, Class exceptionType)
    {
        while (t != null)
        {
            if (exceptionType.isAssignableFrom(t.getClass()))
            {
                return t;
            }

            t = t.getCause();
        }

        return null;
    }

    /**
     * The initialise method is call every time the Exception stategy is assigned to
     * a service or connector. This implementation ensures that initialise is called
     * only once. The actual initialisation code is contained in the
     * <code>doInitialise()</code> method.
     * 
     * @throws InitialisationException
     */
    public final synchronized void initialise() throws InitialisationException
    {
        super.initialise();
        if (!initialised.get())
        {
            doInitialise(muleContext);
            initialised.set(true);
        }
    }

    protected void doInitialise(MuleContext muleContext) throws InitialisationException
    {
        logger.info("Initialising exception listener: " + toString());
    }

    /**
     * If there is a current transaction this method will mark it for rollback This
     * method should not be called if an event is routed from this exception handler
     * to an endpoint that should take part in the current transaction
     */
    protected void handleTransaction(Throwable t)
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();

        if (tx == null)
        {
            return;
        }
        // Work with the root exception, not anything thaat wraps it
        t = ExceptionHelper.getRootException(t);

        if (rollbackTxFilter == null && commitTxFilter == null)
        {
            // By default, rollback the transaction
            rollbackTransaction();
        }
        else if (rollbackTxFilter != null && rollbackTxFilter.accept(t.getClass().getName()))
        {
            // the rollback filter take preceedence over th ecommit filter
            rollbackTransaction();
        }
        else if (commitTxFilter != null && !commitTxFilter.accept(t.getClass().getName()))
        {
            // we only have to rollback if the commitTxFilter does NOT match
            rollbackTransaction();
        }
    }

    protected void rollbackTransaction()
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        try
        {
            if (tx != null)
            {
                tx.setRollbackOnly();
            }
        }
        catch (TransactionException e)
        {
            logException(e);
        }
    }

    /**
     * Routes the current exception to an error endpoint such as a Dead Letter Queue
     * (jms) This method is only invoked if there is a MuleMessage available to
     * dispatch. The message dispatched from this method will be an
     * <code>ExceptionMessage</code> which contains the exception thrown the
     * MuleMessage and any context information.
     * 
     * @param message the MuleMessage being processed when the exception occurred
     * @param target optional; the endpoint being dispatched or received on
     *            when the error occurred. This is NOT the endpoint that the message
     *            will be disptched on and is only supplied to this method for
     *            logging purposes
     * @param t the exception thrown. This will be sent with the ExceptionMessage
     * @see ExceptionMessage
     */
    protected void routeException(MuleMessage message, MessageProcessor target, Throwable t)
    {
        List endpoints = getMessageProcessors(t);
        if (CollectionUtils.isNotEmpty(endpoints))
        {
            try
            {
                logger.error("Message being processed is: " + (message == null ? "null" : message.toString()));
                MuleEventContext ctx = RequestContext.getEventContext();
                String component = "Unknown";
                EndpointURI endpointUri = null;
                if (ctx != null)
                {
                    if (ctx.getFlowConstruct() != null)
                    {
                        component = ctx.getFlowConstruct().getName();
                    }
                    endpointUri = ctx.getEndpointURI();
                }
                else if (target instanceof ImmutableEndpoint)
                {
                    endpointUri = ((ImmutableEndpoint)target).getEndpointURI();
                }
                
                ExceptionMessage msg = new ExceptionMessage(message.getPayload(), t, component, endpointUri);
                MuleMessage exceptionMessage;
                if (ctx == null)
                {
                    exceptionMessage = new DefaultMuleMessage(msg, muleContext);
                }
                else
                {
                    exceptionMessage = new DefaultMuleMessage(msg, ctx.getMessage(), muleContext);
                }

                if (ctx != null && ctx.getFlowConstruct() != null && ctx.getFlowConstruct() instanceof Service)
                {
                    OutboundRouter router = createOutboundRouter();
                    router.process(new DefaultMuleEvent(exceptionMessage, RequestContext.getEvent()));
                }
                else
                {
                    // As the service is not available an outbound router cannot be
                    // used to route the exception message.
                    customRouteExceptionMessage(exceptionMessage);
                }
            }
            catch (Exception e)
            {
                logFatal(message, e);
                closeStream(message);
            }
        }
        else
        {
            handleTransaction(t);
            closeStream(message);
        }
    }

    private void customRouteExceptionMessage(MuleMessage exceptionMessage)
        throws MessagingException, MuleException, DispatchException
    {
        // This is required because we don't always have the service available which
        // is required to use an outbound route. This approach doesn't
        // support everything but rather is an intermediate improvement.
        int numProcessors = messageProcessors.size();
        for (int i = 0; i < numProcessors; i++)
        {
            MessageProcessor processor = messageProcessors.get(i);
            if (numProcessors > 1 && ((DefaultMuleMessage) exceptionMessage).isConsumable())
            {
                throw new MessagingException(
                    CoreMessages.cannotCopyStreamPayload(exceptionMessage.getPayload().getClass().getName()),
                    exceptionMessage);
            }

            MuleMessage clonedMessage = new DefaultMuleMessage(exceptionMessage.getPayload(),
                exceptionMessage, muleContext);
            MuleEvent exceptionEvent = null;
            if (processor instanceof OutboundEndpoint)
            {
                exceptionEvent = new DefaultMuleEvent(clonedMessage, (OutboundEndpoint) processor,
                    new DefaultMuleSession(muleContext));
            }
            else
            {
                exceptionEvent = new DefaultMuleEvent(clonedMessage, RequestContext.getEvent().getEndpoint(),
                    new DefaultMuleSession(muleContext));
            }
            exceptionEvent = RequestContext.setEvent(exceptionEvent);

            processor.process(exceptionEvent);

            if (logger.isDebugEnabled())
            {
                logger.debug("routed Exception message via " + processor);
            }
        }
    }

    protected OutboundRouter createOutboundRouter() throws MuleException
    {
        // Use an instance of OutboundPassThroughRouter but override creation of
        // createTransactionTemplate to use a custom ExceptionListener so that
        // exception handling will not loop forever.
        // We cannot use PassthroughRouter because multiple targets are supported
        // on exception strategies.
        MulticastingRouter router = new MulticastingRouter()
        {
            @Override
            protected void setMessageProperties(FlowConstruct session,
                                                MuleMessage message,
                                                MessageProcessor target)
            {
                // No reply-to or correlation for exception targets, at least for
                // now anyway.
            }
        };
        router.setRoutes(new ArrayList<MessageProcessor>(getMessageProcessors()));
        router.setMuleContext(muleContext);
        return router;
    }

    protected void closeStream(MuleMessage message)
    {
        if (muleContext == null || muleContext.isDisposing() || muleContext.isDisposed())
        {
            return;
        }
        if (message != null
            && muleContext.getRegistry().lookupObject(MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE) != null)
        {
            ((StreamCloserService) muleContext.getRegistry().lookupObject(
                    MuleProperties.OBJECT_MULE_STREAM_CLOSER_SERVICE)).closeStream(message.getPayload());
        }
    }

    /**
     * Returns an endpoint for the given exception. ExceptionListeners can have
     * multiple targets registered on them. This methods allows custom
     * implementations to control which endpoint is used based on the exception
     * thrown. This implementation simply returns the first endpoint in the list.
     * 
     * @param t the exception thrown
     * @return The endpoint used to dispatch an exception message on or null if there
     *         are no targets registered
     */
    protected List<MessageProcessor> getMessageProcessors(Throwable t)
    {
        if (!messageProcessors.isEmpty())
        {
            return messageProcessors;
        }
        else
        {
            return null;
        }
    }

    /**
     * Used to log the error passed into this Exception Listener
     * 
     * @param t the exception thrown
     */
    protected void logException(Throwable t)
    {
        MuleException muleException = ExceptionHelper.getRootMuleException(t);
        if (muleException != null)
        {
            logger.error(muleException.getDetailedMessage());
        }
        else
        {
            logger.error("Caught exception in Exception Strategy: " + t.getMessage(), t);
        }
    }

    /**
     * Logs a fatal error message to the logging system. This should be used mostly
     * if an error occurs in the exception listener itself. This implementation logs
     * the the message itself to the logs if it is not null
     * 
     * @param message The MuleMessage currently being processed
     * @param t the fatal exception to log
     */
    protected void logFatal(MuleMessage message, Throwable t)
    {
        logger.fatal(
            "Failed to dispatch message to error queue after it failed to process.  This may cause message loss."
                            + (message == null ? "" : "Logging Message here: \n" + message.toString()), t);
    }

    public boolean isInitialised()
    {
        return initialised.get();
    }

    /**
     * Fires a server notification to all registered
     * {@link org.mule.api.context.notification.ExceptionNotificationListener}
     * eventManager.
     * 
     * @param notification the notification to fire.
     */
    protected void fireNotification(ExceptionNotification notification)
    {
        if (muleContext != null)
        {
            muleContext.fireNotification(notification);
        }
        else if (logger.isWarnEnabled())
        {
            logger.debug("MuleContext is not yet available for firing notifications, ignoring event: "
                         + notification);
        }
    }

    public WildcardFilter getCommitTxFilter()
    {
        return commitTxFilter;
    }

    public void setCommitTxFilter(WildcardFilter commitTxFilter)
    {
        this.commitTxFilter = commitTxFilter;
    }

    public boolean isEnableNotifications()
    {
        return enableNotifications;
    }

    public void setEnableNotifications(boolean enableNotifications)
    {
        this.enableNotifications = enableNotifications;
    }

    public WildcardFilter getRollbackTxFilter()
    {
        return rollbackTxFilter;
    }

    public void setRollbackTxFilter(WildcardFilter rollbackTxFilter)
    {
        this.rollbackTxFilter = rollbackTxFilter;
    }
    
    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return messageProcessors;
    }
}
