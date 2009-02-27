/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InvalidEndpointTypeException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.routing.RoutingException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.util.StreamCloserService;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ExceptionNotification;
import org.mule.message.ExceptionMessage;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transaction.TransactionCoordination;
import org.mule.transport.NullPayload;
import org.mule.util.CollectionUtils;

import java.beans.ExceptionListener;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractExceptionListener</code> is a base implementation that custom
 * Exception Listeners can override. It provides template methods for handling the
 * for base types of exceptions plus allows multimple endpoints to be associated with
 * this exception listener and provides an implementation for dispatching exception
 * events from this Listener.
 */
public abstract class AbstractExceptionListener implements ExceptionListener, Initialisable, Disposable, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List endpoints = new CopyOnWriteArrayList();

    protected AtomicBoolean initialised = new AtomicBoolean(false);

    protected MuleContext muleContext;

    protected WildcardFilter rollbackTxFilter;
    protected WildcardFilter commitTxFilter;

    protected boolean enableNotifications = true;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        if (endpoints != null)
        {
            this.endpoints.clear();
            // Ensure all endpoints are outbound endpoints
            // This will go when we start dropping suport for 1.4 and start using 1.5
            for (Iterator it = endpoints.iterator(); it.hasNext();)
            {
                ImmutableEndpoint endpoint = (ImmutableEndpoint) it.next();
                if (!(endpoint instanceof OutboundEndpoint))
                {
                    throw new InvalidEndpointTypeException(CoreMessages.exceptionListenerMustUseOutboundEndpoint(this,
                        endpoint));
                }
            }
            this.endpoints.addAll(endpoints);
        }
        else
        {
            throw new IllegalArgumentException("List of endpoints = null");
        }
    }

    public void addEndpoint(OutboundEndpoint endpoint)
    {
        if (endpoint != null)
        {
            endpoints.add(endpoint);
        }
    }

    public boolean removeEndpoint(OutboundEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public void exceptionThrown(Exception e)
    {
        if (enableNotifications)
        {
            fireNotification(new ExceptionNotification(e));
        }

        logException(e);
        handleTransaction(e);

        Throwable t = getExceptionType(e, RoutingException.class);
        if (t != null)
        {
            RoutingException re = (RoutingException) t;
            handleRoutingException(re.getMuleMessage(), re.getEndpoint(), e);
            return;
        }

        t = getExceptionType(e, MessagingException.class);
        if (t != null)
        {
            MessagingException me = (MessagingException) t;
            handleMessagingException(me.getMuleMessage(), e);
            return;
        }

        t = getExceptionType(e, LifecycleException.class);
        if (t != null)
        {
            LifecycleException le = (LifecycleException) t;
            handleLifecycleException(le.getComponent(), e);
            if (RequestContext.getEventContext() != null)
            {
                handleMessagingException(RequestContext.getEventContext().getMessage(), e);
            }
            else
            {
                logger.info("There is no current event available, routing Null message with the exception");
                handleMessagingException(new DefaultMuleMessage(NullPayload.getInstance()), e);
            }
            return;
        }

        handleStandardException(e);
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
     * a service or connector. This implementation ensures that initialise is
     * called only once. The actual initialisation code is contained in the
     * <code>doInitialise()</code> method.
     *
     * @throws InitialisationException
     */
    public final synchronized void initialise() throws InitialisationException
    {
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
        //Work with the root exception, not anything thaat wraps it
        t = ExceptionHelper.getRootException(t);

        if (rollbackTxFilter == null && commitTxFilter == null)
        {
            //By default, rollback the transaction
            rollbackTransaction();
        }
        else if (rollbackTxFilter != null && rollbackTxFilter.accept(t.getClass().getName()))
        {
            //the rollback filter take preceedence over th ecommit filter
            rollbackTransaction();
        }
        else if (commitTxFilter != null && !commitTxFilter.accept(t.getClass().getName()))
        {
            //we only have to rollback if the commitTxFilter does NOT match
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
     * @param message        the MuleMessage being processed when the exception occurred
     * @param failedEndpoint optional; the endpoint being dispatched or received on
     *                       when the error occurred. This is NOT the endpoint that the message
     *                       will be disptched on and is only supplied to this method for
     *                       logging purposes
     * @param t              the exception thrown. This will be sent with the ExceptionMessage
     * @see ExceptionMessage
     */
    protected void routeException(MuleMessage message, ImmutableEndpoint failedEndpoint, Throwable t)
    {
        List endpoints = getEndpoints(t);
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
                    if (ctx.getService() != null)
                    {
                        component = ctx.getService().getName();
                    }
                    endpointUri = ctx.getEndpointURI();
                }
                else if (failedEndpoint != null)
                {
                    endpointUri = failedEndpoint.getEndpointURI();
                }
                ExceptionMessage msg;
                msg = new ExceptionMessage(getErrorMessagePayload(message), t, component, endpointUri);

                MuleMessage exceptionMessage;
                if (ctx == null)
                {
                    exceptionMessage = new DefaultMuleMessage(msg);
                }
                else
                {
                    exceptionMessage = new DefaultMuleMessage(msg, ctx.getMessage());
                }

                for (int i = 0; i < endpoints.size(); i++)
                {
                    OutboundEndpoint endpoint = (OutboundEndpoint) endpoints.get(i);
                    MuleEvent exceptionEvent = new DefaultMuleEvent(exceptionMessage, endpoint, new DefaultMuleSession(
                        exceptionMessage, new MuleSessionHandler(), muleContext), true);
                    exceptionEvent = RequestContext.setEvent(exceptionEvent);
                    endpoint.send(exceptionEvent);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("routed Exception message via " + endpoint);
                    }
                }
            }
            catch (MuleException e)
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

    protected Object getErrorMessagePayload(MuleMessage message)
    {
        try
        {
            return message.getPayloadAsString();
        }
        catch (Exception e)
        {
            logException(e);
            logger.info("Failed to read message payload as string, using raw payload");
            return message.getPayload();
        }
    }

    /**
     * Returns an endpoint for the given exception. ExceptionListeners can have
     * multiple endpoints registered on them. This methods allows custom
     * implementations to control which endpoint is used based on the exception
     * thrown. This implementation simply returns the first endpoint in the list.
     *
     * @param t the exception thrown
     * @return The endpoint used to dispatch an exception message on or null if there
     *         are no endpoints registered
     */
    protected List getEndpoints(Throwable t)
    {
        if (!endpoints.isEmpty())
        {
            return endpoints;
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
     * @param t       the fatal exception to log
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


    public void dispose()
    {
        // Template method
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
            logger.debug("MuleContext is not yet available for firing notifications, ignoring event: " + notification);
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

    /**
     * A messaging exception is thrown when an excpetion occurs during normal message
     * processing. A <code>MessagingException</code> holds a reference to the
     * current message that is passed into this method
     *
     * @param message the current message being processed
     * @param e       the top level exception thrown. This may be a Messaging exception or
     *                some wrapper exception
     * @see MessagingException
     */
    public abstract void handleMessagingException(MuleMessage message, Throwable e);

    /**
     * A routing exception is thrown when an excpetion occurs during normal message
     * processing A <code>RoutingException</code> holds a reference to the current
     * message and te endpoint being routing to or from when the error occurred. Both
     * are passed into this method
     *
     * @param message  the current message being processed
     * @param endpoint the endpoint being dispatched to or received from when the
     *                 error occurred
     * @param e        the top level exception thrown. This may be a Messaging exception or
     *                 some wrapper exception
     * @see RoutingException
     */
    public abstract void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable e);

    /**
     * DefaultLifecyclePhase exceptions are thrown when an error occurs during an object's
     * lifecycle call such as start, stop or initialise. The exception contains a
     * reference to the object that failed which can be used for more informative
     * logging.
     *
     * @param component the object that failed during a lifecycle call
     * @param e       the top level exception thrown. This may or may not be the
     *                <code>LifecycleException</code> but a lifecycle exception will be
     *                present in the exception stack.
     * @see LifecycleException
     */
    public abstract void handleLifecycleException(Object component, Throwable e);

    /**
     * A handler for all other exceptions
     *
     * @param e the top level exception thrown
     */
    public abstract void handleStandardException(Throwable e);
    
    
}
