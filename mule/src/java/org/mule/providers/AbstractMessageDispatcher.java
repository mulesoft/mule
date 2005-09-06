/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleRuntimeException;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.RequestContext;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;

import javax.resource.spi.work.Work;
import java.beans.ExceptionListener;

/**
 * <p/> <code>AbstractMessageDispatcher</code> provides a default dispatch (client) support for handling threads
 * lifecycle and validation.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageDispatcher implements UMOMessageDispatcher, ExceptionListener
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Thread pool of Connector sessions
     */
    protected UMOWorkManager workManager = null;

    protected AbstractConnector connector;

    protected boolean disposed = false;

    protected boolean doThreading = true;

    public AbstractMessageDispatcher(AbstractConnector connector)
    {
        init(connector);
    }

    private void init(AbstractConnector connector)
    {
        this.connector = connector;
        if (connector != null) {
            ThreadingProfile profile = connector.getDispatcherThreadingProfile();
            doThreading = profile.isDoThreading();
            if (doThreading) {
                workManager = profile.createWorkManager(connector.getName() + ".dispatcher");
                try {
                    workManager.start();
                } catch (UMOException e) {
                    throw new MuleRuntimeException(new Message(Messages.FAILED_TO_START_X, "WorkManager"), e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.provider.UMOMessageDispatcher#dispatch(org.mule.umo.UMOEvent)
     */
    public final void dispatch(UMOEvent event) throws DispatchException
    {
        event.setSynchronous(false);
        event.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
        RequestContext.setEvent(event);
        // Apply Security filter if one is set
        UMOEndpoint endpoint = event.getEndpoint();
        if (endpoint.getSecurityFilter() != null) {
            try {
                endpoint.getSecurityFilter().authenticate(event);
            } catch (org.mule.umo.security.SecurityException e) {
                logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                connector.handleException(e);
                return;
            } catch (UMOException e) {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        }
        // the security filter may update the payload so we need to get the
        // latest event again
        event = RequestContext.getEvent();

        try {
            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
            if (doThreading && !event.isSynchronous() && tx == null) {
                workManager.scheduleWork(new Worker(event));
            } else {
                doDispatch(event);
            }
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
    }

    public final UMOMessage send(UMOEvent event) throws DispatchException
    {
        event.setSynchronous(true);
        event.setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
        RequestContext.setEvent(event);
        // Apply Security filter if one is set
        UMOEndpoint endpoint = event.getEndpoint();
        if (endpoint.getSecurityFilter() != null) {
            try {
                endpoint.getSecurityFilter().authenticate(event);
            } catch (org.mule.umo.security.SecurityException e) {
                logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                connector.handleException(e);
                return event.getMessage();
            } catch (UMOException e) {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        }
        // the security filter may update the payload so we need to get the
        // latest event again
        event = RequestContext.getEvent();
        try {
            UMOMessage result = doSend(event);
            return result;
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.util.ExceptionListener#onException(java.lang.Throwable)
     */
    public void exceptionThrown(Exception e)
    {
        getConnector().handleException(e);

    }

    public final synchronized boolean isDisposed()
    {
        return disposed;
    }

    /**
     * Template method to destroy any resources. some connector will want to
     * cache dispatchers and destroy them themselves
     */
    public final synchronized void dispose()
    {
        if (!disposed) {
            try {
                doDispose();
                if (workManager != null) {
                    workManager.dispose();
                }
            } finally {
                connector.getDispatchers().values().remove(this);
                disposed = true;
            }
        }

    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public abstract void doDispose();

    public abstract void doDispatch(UMOEvent event) throws Exception;

    public abstract UMOMessage doSend(UMOEvent event) throws Exception;

    private class Worker implements Work
    {
        private UMOEvent event;

        public Worker(UMOEvent event)
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
            try {
                RequestContext.setEvent(event);
                doDispatch(event);
            } catch (Exception e) {
                getConnector().handleException(e);
            }
        }

        public void release()
        {
        }
    }

    /**
     * RemoteSync causes the message dispatch to wait for a response to an event on a response channel
     * after it sends the event.  The following rules apply to RemoteSync
     * 1.  The connector has to support remoteSync. Some transports do not have the notion of a response channel
     * 2. Check if the endpoint has been configured for remoteSync
     * 3. Check if the REMOTE_SYNC message header has been set
     * 4. Finally, if the current component has a response router configured, that the router will handle the
     * response channel event and we should not try and receive a response in the Message dispatcher
     *
     * If remotesync should not be used we must remove the REMOTE_SYNC header
     *
     * Note the MuleClient will automatically set the REMOTE_SYNC header when client.send(..) is called so that
     * results are returned from remote invocations too.
     * @param event the current event
     * @return true if a response channel should be used to get a resposne from the event dispatch.
     */
    protected boolean useRemoteSync(UMOEvent event) {
        boolean remoteSync = false;
        if(event.getEndpoint().getConnector().isRemoteSyncEnabled()) {
            remoteSync = event.getEndpoint().isRemoteSync() ||
                    event.getMessage().getBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, false);
            if(remoteSync) {
                //component will be null for client calls
                if(event.getComponent()!=null) {
                    remoteSync = event.getComponent().getDescriptor().getResponseRouter() == null;
                }
            }
        }
        if(!remoteSync) {
            event.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
            event.getMessage().removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
        }
        return remoteSync;
    }
}
