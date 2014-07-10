/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.GlobalNameableObject;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.RollbackSourceCallback;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.SecurityException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.config.ExceptionHelper;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.ServiceStatistics;
import org.mule.message.ExceptionMessage;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.routing.filters.WildcardFilter;
import org.mule.routing.outbound.MulticastingRouter;
import org.mule.transaction.TransactionCoordination;
import org.mule.util.CollectionUtils;
import org.mule.util.StringUtils;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the base class for exception strategies which contains several helper methods.  However, you should
 * probably inherit from <code>AbstractMessagingExceptionStrategy</code> (if you are creating a Messaging Exception Strategy)
 * or <code>AbstractSystemExceptionStrategy</code> (if you are creating a System Exception Strategy) rather than directly from this class.
 */
public abstract class AbstractExceptionListener extends AbstractMessageProcessorOwner implements GlobalNameableObject
{

    protected static final String NOT_SET = "<not set>";

    protected transient Log logger = LogFactory.getLog(getClass());

    protected List<MessageProcessor> messageProcessors = new CopyOnWriteArrayList<MessageProcessor>();

    protected AtomicBoolean initialised = new AtomicBoolean(false);

    protected WildcardFilter rollbackTxFilter;
    protected WildcardFilter commitTxFilter;

    protected boolean enableNotifications = true;

    protected String globalName;

    @Override
    public String getGlobalName()
    {
        return globalName;
    }

    @Override
    public void setGlobalName(String globalName)
    {
        this.globalName = globalName;
    }

    public AbstractExceptionListener()
    {
        super.setMessagingExceptionHandler(new MessagingExceptionHandlerToSystemAdapter());
    }


    protected boolean isRollback(Throwable t)
    {
        // Work with the root exception, not anything thaat wraps it
        t = ExceptionHelper.getRootException(t);
        if (rollbackTxFilter == null && commitTxFilter == null)
        {
            return true;
        }
        else
        {
            return (rollbackTxFilter != null && rollbackTxFilter.accept(t.getClass().getName()))
                   || (commitTxFilter != null && !commitTxFilter.accept(t.getClass().getName()));
        }
    }

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

    protected Throwable getExceptionType(Throwable t, Class<? extends Throwable> exceptionType)
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
    @Override
    public final synchronized void initialise() throws InitialisationException
    {
        if (!initialised.get())
        {
            super.initialise();
            doInitialise(muleContext);
            initialised.set(true);
        }
    }

    protected void doInitialise(MuleContext context) throws InitialisationException
    {
        logger.info("Initialising exception listener: " + toString());
    }

    protected void fireNotification(Exception ex)
    {
        if (enableNotifications)
        {
            if (ex instanceof SecurityException)
            {
                fireNotification(new SecurityNotification((SecurityException) ex, SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
            }
            else
            {
                fireNotification(new ExceptionNotification(ex));
            }
        }
    }

    /**
     * Routes the current exception to an error endpoint such as a Dead Letter Queue
     * (jms) This method is only invoked if there is a MuleMessage available to
     * dispatch. The message dispatched from this method will be an
     * <code>ExceptionMessage</code> which contains the exception thrown the
     * MuleMessage and any context information.
     *
     * @param event the MuleEvent being processed when the exception occurred
     * @param t the exception thrown. This will be sent with the ExceptionMessage
     * @see ExceptionMessage
     */
    protected void routeException(MuleEvent event, Throwable t)
    {
        if (!messageProcessors.isEmpty())
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Message being processed is: " + (event.getMessage().getPayloadForLogging()));
                }
                String component = "Unknown";
                if (event.getFlowConstruct() != null)
                {
                    component = event.getFlowConstruct().getName();
                }
                URI endpointUri = event.getMessageSourceURI();

                // Create an ExceptionMessage which contains the original payload, the exception, and some additional context info.
                ExceptionMessage msg = new ExceptionMessage(event, t, component, endpointUri);
                MuleMessage exceptionMessage = new DefaultMuleMessage(msg, event.getMessage(), muleContext);

                // Create an outbound router with all endpoints configured on the exception strategy
                MulticastingRouter router = new MulticastingRouter()
                {
                    @Override
                    protected void setMessageProperties(FlowConstruct session, MuleMessage message, MessageProcessor target)
                    {
                        // No reply-to or correlation for exception targets, at least for now anyway.
                    }
                };
                router.setRoutes(getMessageProcessors());
                router.setMuleContext(muleContext);

                // Route the ExceptionMessage to the new router
                router.route(new DefaultMuleEvent(exceptionMessage, event));
            }
            catch (Exception e)
            {
                logFatal(event, e);
            }
        }

        processOutboundRouterStatistics(event.getFlowConstruct());
    }

    /*
     * Kept for backward compatibility
     */
    /**
     * @deprecated use {@link #routeException(org.mule.api.MuleEvent, Throwable)} instead
     */
    @Deprecated
    protected void routeException(MuleEvent event, MessageProcessor target, Throwable t)
    {
        routeException(event,t);
    }

    /*
     * Kept for backward compatibility
     */
    /**
     * @deprecated use {@link #rollback(Exception)} instead
     */
    @Deprecated
    protected void rollback(RollbackSourceCallback rollbackMethod)
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            try
            {
                tx.rollback();

                // TODO The following was in the catch clause of TransactionTemplate previously.
                // Do we need to do this here?  If so, where can we store these variables (suspendedXATx, joinedExternal)
                // so that they are available to us in the exception handler?
                //
                //if (suspendedXATx != null)
                //{
                //  resumeXATransaction(suspendedXATx);
                //}
                //if (joinedExternal != null)
                //{
                //    TransactionCoordination.getInstance().unbindTransaction(joinedExternal);
                //}
            }
            catch (TransactionException e)
            {
                logger.error(e);
            }
        }
        else if (rollbackMethod != null)
        {
            rollbackMethod.rollback();
        }
    }

    protected void closeStream(MuleMessage message)
    {
        if (muleContext == null || muleContext.isDisposing() || muleContext.isDisposed())
        {
            return;
        }
        if (message != null)
        {
            muleContext.getStreamCloserService().closeStream(message.getPayload());
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
     * @param event The MuleEvent currently being processed
     * @param t the fatal exception to log
     */
    protected void logFatal(MuleEvent event, Throwable t)
    {
        FlowConstructStatistics statistics = event.getFlowConstruct().getStatistics();
        if (statistics != null && statistics.isEnabled())
        {
            statistics.incFatalError();
        }

        MuleMessage logMessage = event.getMessage();
        String logUniqueId = StringUtils.defaultString(logMessage.getUniqueId(), NOT_SET);
        String correlationId = StringUtils.defaultString(logMessage.getCorrelationId(), NOT_SET);
        int correlationGroupSize = logMessage.getCorrelationGroupSize();
        int correlationGroupSeq = logMessage.getCorrelationSequence();

        String printableLogMessage = MessageFormat.format("Message identification summary here: " +
                "id={0} correlationId={1}, correlationGroup={2}, correlationSeq={3}",
                logUniqueId, correlationId, correlationGroupSize, correlationGroupSeq);

        logger.fatal(
            "Failed to dispatch message to error queue after it failed to process.  This may cause message loss. "
                            + (event.getMessage() == null ? "" : printableLogMessage), t);
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
    protected void fireNotification(ServerNotification notification)
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

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        return;
    }

    /*
     * kept for backward compatibility
     */
     /**
     * @deprecated use {@link #rollback(Exception)} instead.
     * parameter should be null
     */
    @Deprecated
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

    /*
     * Kept for backward compatibility
     */
    /**
     * If there is a current transaction this method will mark it for rollback This
     * method should not be called if an event is routed from this exception handler
     * to an endpoint that should take part in the current transaction
     *
     * @deprecated this method should not be used anymore. Transactions must be handled by provided ExceptionStrategy
     */
    @Deprecated
    protected void handleTransaction(Throwable t)
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction();

        if (tx == null)
        {
            return;
        }
        // Work with the root exception, not anything thaat wraps it
        t = ExceptionHelper.getRootException(t);
        boolean transactionRollback = false;

        if (rollbackTxFilter == null && commitTxFilter == null)
        {
            // By default, rollback the transaction
            rollbackTransaction();
            transactionRollback = true;
        }
        else if (rollbackTxFilter != null && rollbackTxFilter.accept(t.getClass().getName()))
        {
            // the rollback filter take preceedence over th ecommit filter
            rollbackTransaction();
            transactionRollback = true;
        }
        else if (commitTxFilter != null && !commitTxFilter.accept(t.getClass().getName()))
        {
            // we only have to rollback if the commitTxFilter does NOT match
            rollbackTransaction();
            transactionRollback = true;
        }

        if (transactionRollback && t instanceof MessagingException)
        {
            ((MessagingException)t).setCauseRollback(true);
        }
    }

    protected void commit()
    {
        TransactionCoordination.getInstance().commitCurrentTransaction();
    }

    protected void rollback(Exception ex)
    {
        if (TransactionCoordination.getInstance().getTransaction() != null)
        {
            TransactionCoordination.getInstance().rollbackCurrentTransaction();
        }
        if (ex instanceof MessagingException)
        {
            MessagingException messagingException = (MessagingException) ex;
            messagingException.setCauseRollback(true);
        }
    }

    void processOutboundRouterStatistics(FlowConstruct construct)
    {
        List<MessageProcessor> processors = getMessageProcessors();
        FlowConstructStatistics statistics = construct.getStatistics();
        if (CollectionUtils.isNotEmpty(processors) && statistics instanceof ServiceStatistics)
        {
            if (statistics.isEnabled())
            {
                for (MessageProcessor endpoint : processors)
                {
                    ((ServiceStatistics) statistics).getOutboundRouterStat().incrementRoutedMessage(endpoint);
                }
            }
        }
    }
}
