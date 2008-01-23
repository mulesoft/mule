/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.ResponseRouterCollection;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.context.notification.MessageNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.transaction.TransactionCoordination;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;

/**
 * Provide a default dispatch (client) support for handling threads lifecycle and validation.
 */
public abstract class AbstractMessageDispatcher extends AbstractConnectable implements MessageDispatcher
{

    public AbstractMessageDispatcher(ImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.transport.MessageDispatcher#dispatch(org.mule.api.MuleEvent)
     */
    public final void dispatch(MuleEvent event) throws DispatchException
    {
        event.setSynchronous(false);
        event.getMessage().setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY,
            event.getEndpoint().getEndpointURI().toString());
        event = OptimizedRequestContext.criticalSetEvent(event); // MULE-2112

        // Apply Security filter if one is set
        ImmutableEndpoint endpoint = event.getEndpoint();
        if (endpoint.getSecurityFilter() != null)
        {
            try
            {
                endpoint.getSecurityFilter().authenticate(event);
            }
            catch (org.mule.api.security.SecurityException e)
            {
                // TODO MULE-863: Do we need this warning?
                logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                connector.fireNotification(new SecurityNotification(e,
                    SecurityNotification.ADMIN_EVENT_ACTION_START_RANGE));
                connector.handleException(e);
                return;
            }
            catch (MuleException e)
            {
                disposeAndLogException();
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        }

        try
        {
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (isDoThreading() && !event.isSynchronous() && tx == null)
            {
                workManager.scheduleWork(new Worker(event), WorkManager.INDEFINITE, null, connector);
            }
            else
            {
                // Make sure we are connected
                connectionStrategy.connect(this);
                doDispatch(event);
                if (connector.isEnableMessageEvents())
                {
                    String component = null;
                    if (event.getComponent() != null)
                    {
                        component = event.getComponent().getName();
                    }
                    connector.fireNotification(new MessageNotification(event.getMessage(), event
                        .getEndpoint(), component, MessageNotification.MESSAGE_DISPATCHED));
                }
            }
        }
        catch (DispatchException e)
        {
            disposeAndLogException();
            throw e;
        }
        catch (Exception e)
        {
            disposeAndLogException();
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
    }

    public final MuleMessage send(MuleEvent event) throws DispatchException
    {
        // No point continuing if the component has rolledback the transaction
        if (isTransactionRollback())
        {
            return event.getMessage();
        }

        event.setSynchronous(true);
        event.getMessage().setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY,
            event.getEndpoint().getEndpointURI().getUri().toString());
        event = OptimizedRequestContext.unsafeSetEvent(event);

        // Apply Security filter if one is set
        ImmutableEndpoint endpoint = event.getEndpoint();
        if (endpoint.getSecurityFilter() != null)
        {
            try
            {
                endpoint.getSecurityFilter().authenticate(event);
            }
            catch (org.mule.api.security.SecurityException e)
            {
                logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                connector.fireNotification(new SecurityNotification(e,
                    SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
                connector.handleException(e);
                return event.getMessage();
            }
            catch (MuleException e)
            {
                disposeAndLogException();
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        }

        try
        {
            // Make sure we are connected
            connectionStrategy.connect(this);

            MuleMessage result = doSend(event);
            if (connector.isEnableMessageEvents())
            {
                String component = null;
                if (event.getComponent() != null)
                {
                    component = event.getComponent().getName();
                }
                connector.fireNotification(new MessageNotification(event.getMessage(), event.getEndpoint(),
                    component, MessageNotification.MESSAGE_SENT));
            }

            // Once a dispatcher has done its work we need to remove this property
            // so that it is not propagated to the next request
            if (result != null
                    && result.getPropertyNames().contains(MuleProperties.MULE_REMOTE_SYNC_PROPERTY))
            {
//                result = RequestContext.safeMessageCopy(result);
                result.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
            }
            return result;
        }
        catch (DispatchException e)
        {
            disposeAndLogException();
            throw e;
        }
        catch (Exception e)
        {
            disposeAndLogException();
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
    }

    /**
     * RemoteSync causes the message dispatch to wait for a response to an event on a
     * response channel after it sends the event. The following rules apply to
     * RemoteSync 1. The connector has to support remoteSync. Some transports do not
     * have the notion of a response channel 2. Check if the endpoint has been
     * configured for remoteSync 3. Check if the REMOTE_SYNC message header has been
     * set 4. Finally, if the current component has a response router configured,
     * that the router will handle the response channel event and we should not try
     * and receive a response in the Message dispatcher If remotesync should not be
     * used we must remove the REMOTE_SYNC header Note the MuleClient will
     * automatically set the REMOTE_SYNC header when client.send(..) is called so
     * that results are returned from remote invocations too.
     *
     * @param event the current event
     * @return true if a response channel should be used to get a resposne from the
     *         event dispatch.
     */
    protected boolean useRemoteSync(MuleEvent event)
    {
        boolean remoteSync = false;
        if (event.getEndpoint().getConnector().isRemoteSyncEnabled())
        {
            remoteSync = event.getEndpoint().isRemoteSync()
                            || event.getMessage().getBooleanProperty(
                                MuleProperties.MULE_REMOTE_SYNC_PROPERTY, false);
            if (remoteSync)
            {
                // component will be null for client calls
                if (event.getComponent() != null)
                {
                    ResponseRouterCollection responseRouters = event.getComponent().getResponseRouter();
                    if (responseRouters != null && responseRouters.hasEndpoints())
                    {
                        remoteSync = false;
                    }
                    else
                    {
                        remoteSync = true;
                    }
                }
            }
        }
        if (!remoteSync)
        {
            event.getMessage().removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
        }
        return remoteSync;
    }

    private class Worker implements Work
    {
        private MuleEvent event;

        public Worker(MuleEvent event)
        {
            this.event = event;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            try
            {
                event = RequestContext.setEvent(event);
                // Make sure we are connected
                connectionStrategy.connect(AbstractMessageDispatcher.this);
                AbstractMessageDispatcher.this.doDispatch(event);

                if (connector.isEnableMessageEvents())
                {
                    String component = null;
                    if (event.getComponent() != null)
                    {
                        component = event.getComponent().getName();
                    }

                    connector.fireNotification(new MessageNotification(event.getMessage(), event
                        .getEndpoint(), component, MessageNotification.MESSAGE_DISPATCHED));
                }
            }
            catch (Exception e)
            {
                AbstractMessageDispatcher.this.getConnector().handleException(e);
            }
        }

        public void release()
        {
            // nothing to do
        }
    }

    /**
     * Checks to see if the current transaction has been rolled back
     * 
     * @return
     */
    protected boolean isTransactionRollback()
    {
        try
        {
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (tx != null && tx.isRollbackOnly())
            {
                return true;
            }
        }
        catch (TransactionException e)
        {
            // TODO MULE-863: What should we really do?
            logger.warn(e.getMessage());
        }
        return false;
    }

    protected abstract void doDispatch(MuleEvent event) throws Exception;

    protected abstract MuleMessage doSend(MuleEvent event) throws Exception;
                                             
}
