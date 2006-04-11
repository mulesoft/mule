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
 */
package org.mule.impl;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.ExceptionHelper;
import org.mule.impl.message.ExceptionMessage;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.MessagingException;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.routing.RoutingException;

import java.beans.ExceptionListener;
import java.util.Iterator;
import java.util.List;

/**
 * <code>AbstractExceptionListener</code> is a base implementation that custom
 * Exception Listeners can override. It provides template methods for handling
 * the for base types of exceptions plus allows multimple endpoints to be
 * associated with this exception listener and provides an implementaiton for
 * dispatching exception events from this Listener.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractExceptionListener implements ExceptionListener, Initialisable
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List endpoints = new CopyOnWriteArrayList();

    public List getEndpoints()
    {
        return endpoints;
    }

    public void setEndpoints(List endpoints)
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();) {
            addEndpoint((UMOEndpoint) iterator.next());
        }
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint != null) {
            endpoint.setType(UMOEndpoint.ENDPOINT_TYPE_SENDER);
            endpoints.add(endpoint);
        }
    }

    public boolean removeEndpoint(UMOEndpoint endpoint)
    {
        return endpoints.remove(endpoint);
    }

    public void exceptionThrown(Exception e)
    {
        Throwable t = getExceptionType(e, RoutingException.class);
        if (t != null) {
            RoutingException re = (RoutingException) t;
            handleRoutingException(re.getUmoMessage(), re.getEndpoint(), e);
            return;
        }

        t = getExceptionType(e, MessagingException.class);
        if (t != null) {
            MessagingException me = (MessagingException) t;
            handleMessagingException(me.getUmoMessage(), e);
            return;
        }

        t = getExceptionType(e, LifecycleException.class);
        if (t != null) {
            LifecycleException le = (LifecycleException) t;
            handleLifecycleException(le.getComponent(), e);
            return;
        }

        handleStandardException(e);
    }

    protected Throwable getExceptionType(Exception e, Class exceptionType)
    {
        Throwable current = e;
        while (current != null) {
            if (exceptionType.isAssignableFrom(e.getClass())) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }

    public void initialise() throws InitialisationException
    {
        logger.info("Initialising exception listener: " + toString());
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();) {
            UMOEndpoint umoEndpoint = (UMOEndpoint) iterator.next();
            umoEndpoint.initialise();
        }
    }

    /**
     * If there is a current transaction this method will mark it for rollback
     * This method should not be called if an event is routed from this
     * exception handler to an endpoint that should take part in the current
     * transaction
     */
    protected void markTransactionForRollback()
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        try {
            if (tx != null) {
                tx.setRollbackOnly();
            }
        } catch (TransactionException e) {
            logException(e);
        }
    }

    /**
     * Routes the current exception to an error endpoint such as a Dead Letter
     * Queue (jms) This method is only invoked if there is a UMOMassage
     * available to dispatch. The message dispatched from this method will be an
     * <code>ExceptionMessage</code> which contains the exception thrown the
     * UMOMessage and any context information.
     * 
     * @param message the UMOMessage being processed when the exception occurred
     * @param failedEndpoint optional; the endpoint being dispatched or recieved
     *            on when the error occurred. This is NOT the endpoint that the
     *            message will be disptched on and is only supplied to this
     *            method for logging purposes
     * @param t the exception thrown. This will be sent with the
     *            ExceptionMessage
     *
     * @see ExceptionMessage
     */
    protected void routeException(UMOMessage message, UMOImmutableEndpoint failedEndpoint, Throwable t)
    {
        UMOEndpoint endpoint = getEndpoint(t);
        if (endpoint != null) {
            try {
                logger.error("Message being processed is: " + (message == null ? "null" : message.toString()));
                UMOEventContext ctx = RequestContext.getEventContext();
                String component = "Unknown";
                UMOEndpointURI endpointUri = null;
                if(ctx!=null) {
                    if(ctx.getComponentDescriptor()!=null) {
                        component = ctx.getComponentDescriptor().getName();
                    }
                    endpointUri = ctx.getEndpointURI();
                } else if(failedEndpoint!=null) {
                    endpointUri = failedEndpoint.getEndpointURI();
                }
                ExceptionMessage msg = null;
                msg = new ExceptionMessage(getErrorMessagePayload(message), t, component, endpointUri);

                UMOMessage exceptionMessage = null;
                if(ctx==null) {
                    exceptionMessage = new MuleMessage(msg);
                } else {
                    exceptionMessage = new MuleMessage(msg, ctx.getMessage());
                }
                UMOEvent exceptionEvent = new MuleEvent(exceptionMessage, endpoint,
                        new MuleSession(exceptionMessage, new MuleSessionHandler()), true);
                RequestContext.setEvent(exceptionEvent);
                endpoint.getConnector().getDispatcher(endpoint).send(exceptionEvent);

                if (logger.isDebugEnabled()) {
                    logger.debug("routed Exception message via " + endpoint);
                }

            } catch (UMOException e) {
                logFatal(message, e);
            }
        } else {
            markTransactionForRollback();
        }
    }

    protected Object getErrorMessagePayload(UMOMessage message)
    {
        try {
            return message.getPayloadAsString();
        } catch (Exception e) {
            logException(e);
            logger.info("Failed to read message payload as string, using raw payload");
            return message.getPayload();
        }
    }

    /**
     * Returns an endpoint for the given exception. ExceptionListeners can have
     * multiple endpoints registered on them. This methods allows custom
     * implementations to control which endpoint is used based on the exception
     * thrown. This implementation simply returns the first endpoint in the
     * list.
     * 
     * @param t the exception thrown
     * @return The endpoint used to dispatch an exception message on or null if
     *         there are no endpoints registered
     */
    protected UMOEndpoint getEndpoint(Throwable t)
    {
        if (endpoints.size() > 0) {
            return (UMOEndpoint) endpoints.get(0);
        } else {
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
        UMOException umoe = ExceptionHelper.getRootMuleException(t);
        if (umoe != null) {
            logger.error(umoe.getDetailedMessage());
        } else {
            logger.error("Caught exception in Exception Strategy: " + t.getMessage(), t);
        }
    }

    /**
     * Logs a fatal error message to the logging system. This should be used
     * mostly if an error occurs in the exception listener itself. This
     * implementation logs the the message itself to the logs if it is not null
     * 
     * @param message The UMOMessage currently being processed
     * @param t the fatal exception to log
     */
    protected void logFatal(UMOMessage message, Throwable t)
    {
        logger.fatal("Failed to dispatch message to error queue after it failed to process.  This may cause message loss."
                             + (message == null ? "" : "Logging Message here: \n" + message.toString()),
                     t);
    }

    /**
     * A messaging exception is thrown when an excpetion occurs during normal
     * message processing. A <code>MessagingException</code> holds a reference
     * to the current message that is passed into this method
     * 
     * @param message the current message being processed
     * @param e the top level exception thrown. This may be a Messaging
     *            exception or some wrapper exception
     * @see MessagingException
     */
    public abstract void handleMessagingException(UMOMessage message, Throwable e);

    /**
     * A routing exception is thrown when an excpetion occurs during normal
     * message processing A <code>RoutingException</code> holds a reference to
     * the current message and te endpoint being routing to or from when the
     * error occurred. Both are passed into this method
     * 
     * @param message the current message being processed
     * @param endpoint the endpoint being dispatched to or received from when
     *            the error occurred
     * @param e the top level exception thrown. This may be a Messaging
     *            exception or some wrapper exception
     * @see RoutingException
     */
    public abstract void handleRoutingException(UMOMessage message, UMOImmutableEndpoint endpoint, Throwable e);

    /**
     * Lifecycle exceptions are thrown when an error occurs during an object's
     * lifecycle call such as start, stop or initialise. The exception contains
     * a reference to the object that failed which can be used for more
     * informative logging.
     * 
     * @param component the object that failed during a lifecycle call
     * @param e the top level exception thrown. This may or may not be the
     *            <code>LifecycleException</code> but a lifecycle exception
     *            will be present in the exception stack.
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
