/*
 * $Id$
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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleRuntimeException;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.RequestContext;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.impl.internal.notifications.MessageNotification;
import org.mule.impl.internal.notifications.SecurityNotification;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.util.concurrent.WaitableBoolean;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkManager;

import java.beans.ExceptionListener;
import java.io.OutputStream;

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

    protected UMOImmutableEndpoint endpoint;
    protected AbstractConnector connector;

    protected boolean disposed = false;

    protected boolean doThreading = true;

    protected ConnectionStrategy connectionStrategy;

    protected WaitableBoolean connected = new WaitableBoolean(false);

    private AtomicBoolean connecting = new AtomicBoolean(false);

    public AbstractMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        this.connector = (AbstractConnector)endpoint.getConnector();
        connectionStrategy = connector.getConnectionStrategy();
        if(connectionStrategy instanceof AbstractConnectionStrategy) {
            //We don't want to do threading in the dispatcher because we're either
            //already running in a worker thread (asynchronous) or we need to complete the operation
            // in a single thread
            ((AbstractConnectionStrategy)connectionStrategy).setDoThreading(false);
        }

        ThreadingProfile profile = connector.getDispatcherThreadingProfile();
        doThreading = profile.isDoThreading();
        if (doThreading) {
            workManager = connector.createDispatcherWorkManager(connector.getName() + ".dispatchers");
            try {
                workManager.start();
            } catch (UMOException e) {
                dispose();
                throw new MuleRuntimeException(new Message(Messages.FAILED_TO_START_X, "WorkManager"), e);
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
        try {
            event.setSynchronous(false);
            event.getMessage().setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
            RequestContext.setEvent(event);
            // Apply Security filter if one is set
            UMOImmutableEndpoint endpoint = event.getEndpoint();
            if (endpoint.getSecurityFilter() != null) {
                try {
                    endpoint.getSecurityFilter().authenticate(event);
                } catch (org.mule.umo.security.SecurityException e) {
                    logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                    connector.fireNotification(new SecurityNotification(e, SecurityNotification.ADMIN_EVENT_ACTION_START_RANGE));
                    connector.handleException(e);
                    return;
                } catch (UMOException e) {
                    dispose();
                    throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
                }
            }
            // the security filter may update the payload so we need to get the
            // latest event again
            event = RequestContext.getEvent();

            try {
                UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
                if (doThreading && !event.isSynchronous() && tx == null) {
                    workManager.scheduleWork(new Worker(event), WorkManager.IMMEDIATE, null, connector);
                } else {
                    //Make sure we are connected
                    connectionStrategy.connect(this);
                    doDispatch(event);
                    if(connector.isEnableMessageEvents()) {
                        String component = null;
                        if(event.getComponent()!=null) {
                            component = event.getComponent().getDescriptor().getName();
                        }
                        connector.fireNotification(new MessageNotification(event.getMessage(), event.getEndpoint(), component, MessageNotification.MESSAGE_DISPATCHED));
                    }
                }
            } catch (DispatchException e) {
                dispose();
                throw e;
            } catch (Exception e) {
                dispose();
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        } finally {
            if(connector.isCreateDispatcherPerRequest()) {
                dispose();
            }
        }
    }

    public final UMOMessage send(UMOEvent event) throws DispatchException
    {
        try {
            event.setSynchronous(true);
            event.getMessage().setProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, event.getEndpoint().getEndpointURI().toString());
            RequestContext.setEvent(event);
            // Apply Security filter if one is set
            UMOImmutableEndpoint endpoint = event.getEndpoint();
            if (endpoint.getSecurityFilter() != null) {
                try {
                    endpoint.getSecurityFilter().authenticate(event);
                } catch (org.mule.umo.security.SecurityException e) {
                    logger.warn("Outbound Request was made but was not authenticated: " + e.getMessage(), e);
                    connector.fireNotification(new SecurityNotification(e, SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
                    connector.handleException(e);
                    return event.getMessage();
                } catch (UMOException e) {
                    dispose();
                    throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
                }
            }
            // the security filter may update the payload so we need to get the
            // latest event again
            event = RequestContext.getEvent();
            try {
                //Make sure we are connected
                connectionStrategy.connect(this);

                UMOMessage result = doSend(event);
                if(connector.isEnableMessageEvents()) {
                    String component = null;
                    if(event.getComponent()!=null) {
                        component = event.getComponent().getDescriptor().getName();
                    }
                    connector.fireNotification(new MessageNotification(event.getMessage(), event.getEndpoint(), component, MessageNotification.MESSAGE_SENT));
                }
                //Once a dispatcher has done its work we need to romve this property so that
                //it is not propagated to the next request
                if(result!=null) {
                    result.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
                }
                return result;
            } catch (DispatchException e) {
                dispose();
                throw e;
            } catch (Exception e) {
                dispose();
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        } finally {
            if(connector.isCreateDispatcherPerRequest()) {
                dispose();
            }
        }
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpointUri the endpoint URI to use when connecting to the resource
     * @param timeout     the maximum time the operation should block before returning. The call should
     *                    return immediately if there is data available. If no data becomes available before the timeout
     *                    elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     *         avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     *                   //@deprecated Use receive(UMOImmutableEndpoint endpoint, long timeout)
     */
    public final UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        return receive(new ImmutableMuleEndpoint(endpointUri.toString(), true), timeout);
    }

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout  the maximum time the operation should block before returning. The call should
     *                 return immediately if there is data available. If no data becomes available before the timeout
     *                 elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     *         avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    public final UMOMessage receive(UMOImmutableEndpoint endpoint, long timeout) throws Exception {

        try {

            try {
                //Make sure we are connected
                connectionStrategy.connect(this);
                UMOMessage result = doReceive(endpoint, timeout);
                if(result !=null && connector.isEnableMessageEvents()) {
                    String component = null;
                    connector.fireNotification(new MessageNotification(result, endpoint, component, MessageNotification.MESSAGE_RECEIVED));
                }
                return result;
            } catch (DispatchException e) {
                dispose();
                throw e;
            } catch (Exception e) {
                dispose();
                throw new ReceiveException(endpoint, timeout, e);
            }
        } finally {
            if(connector.isCreateDispatcherPerRequest()) {
                dispose();
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.util.ExceptionListener#onException(java.lang.Throwable)
     */
    public void exceptionThrown(Exception e)
    {
        try {
            getConnector().handleException(e);
        } finally {
            dispose();
        }

    }

    public final boolean isDisposed()
    {
        return disposed;
    }

    /**
     * Template method to destroy any resources held by the Message Dispatcher
     */
    public final synchronized void dispose()
    {
        if (!disposed) {
            try {
                try {
                    disconnect();
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
                doDispose();
                if (workManager != null) {
                    workManager.dispose();
                }
            } finally {
                disposed = true;
            }
        }
    }

    public UMOConnector getConnector()
    {
        return connector;
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
            event.getMessage().removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
        }
        return remoteSync;
    }

    /**
     * Well get the output stream (if any) for this type of transport.  Typically this will be called only when Streaming
     * is being used on an outbound endpoint
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message  the current message being processed
     * @return the output stream to use for this request or null if the transport does not support streaming
     * @throws org.mule.umo.UMOException
     */
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message) throws UMOException {
        return null;
    }

    public void connect() throws Exception {
        if (connected.get()) {
            return;
        }
        if(disposed) {
            if(logger.isWarnEnabled()) {
                logger.warn("Dispatcher has been disposed. Cannot connector resource");
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Attempting to connect to: " + endpoint.getEndpointURI());
        }
        if (connecting.compareAndSet(false, true)) {
            connectionStrategy.connect(this);
            logger.info("Successfully connected to: " + endpoint.getEndpointURI());
            return;
        }

        try {
            doConnect(endpoint);
            connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
                    ConnectionNotification.CONNECTION_CONNECTED));
        } catch (Exception e) {
            connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
                    ConnectionNotification.CONNECTION_FAILED));
            if (e instanceof ConnectException) {
                throw (ConnectException) e;
            } else {
                throw new ConnectException(e, this);
            }
        }
        connected.set(true);
        connecting.set(false);
    }

     public void disconnect() throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("Disconnecting from: " + endpoint.getEndpointURI());
        }
        connector.fireNotification(new ConnectionNotification(this, getConnectEventId(endpoint),
                ConnectionNotification.CONNECTION_DISCONNECTED));
        connected.set(false);
        doDisconnect();
        logger.info("Disconnected from: " + endpoint.getEndpointURI());
    }

    protected String getConnectEventId(UMOImmutableEndpoint endpoint)
    {
        return connector.getName() + ".dispatcher (" + endpoint.getEndpointURI() + ")";
    }

    public final boolean isConnected() {
       return connected.get();
   }

    /**
     * Returns a string identifying the underlying resource
     *
     * @return
     */
    public String getConnectionDescription() {
        return endpoint.getEndpointURI().toString();
    }

    public void reconnect() throws Exception {
        disconnect();
        connect();
    }

    protected abstract void doDispose();

    protected abstract void doDispatch(UMOEvent event) throws Exception;

    protected abstract UMOMessage doSend(UMOEvent event) throws Exception;

    protected abstract void doConnect(UMOImmutableEndpoint endpoint) throws Exception;

    protected abstract void doDisconnect() throws Exception;

    /**
     * Make a specific request to the underlying transport
     *
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout  the maximum time the operation should block before returning. The call should
     *                 return immediately if there is data available. If no data becomes available before the timeout
     *                 elapses, null will be returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be returned if no data was
     *         avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected abstract UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception;

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
                //Make sure we are connected
                connectionStrategy.connect(AbstractMessageDispatcher.this);
                doDispatch(event);
                if(connector.isEnableMessageEvents()) {
                    String component = null;
                    if(event.getComponent()!=null) {
                        component = event.getComponent().getDescriptor().getName();
                    }
                    connector.fireNotification(new MessageNotification(event.getMessage(), event.getEndpoint(), component, MessageNotification.MESSAGE_DISPATCHED));
                }
            } catch (Exception e) {
                getConnector().handleException(e);
            }
        }

        public void release()
        {
            // nothing to do
        }
    }
}
