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
 *
 */

package org.mule.providers;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.ExceptionHelper;
import org.mule.config.ThreadingProfile;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleSession;
import org.mule.impl.RequestContext;
import org.mule.impl.ResponseOutputStream;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.security.SecurityException;

import java.io.OutputStream;

/**
 * <code>AbstractMessageReceiver</code> provides common methods for all Message Receivers provided with Mule.
 * A message receiver enables an endpoint to receive a message from an external system.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageReceiver implements UMOMessageReceiver {
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * The Component with which this receiver is associated with
     */
    protected UMOComponent component = null;

    /**
     * The endpoint descriptor which is associated with this receiver
     */
    protected UMOEndpoint endpoint = null;

    /**
     * The model managing the UMO components
     */
    protected UMOModel model = null;

    /**
     * the endpoint to receive events on
     */
    protected UMOConnector connector = null;

    protected boolean serverSide = true;

    protected SynchronizedBoolean disposing = new SynchronizedBoolean(false);

    /**
     * Stores the endpointUri that this receiver listens on.  This enpoint can be different
     * to the endpointUri in the endpoint stored on the receiver as endpoint endpointUri may get
     * rewritten if this endpointUri is a wildcard endpointUri such as jms.*
     */
    private UMOEndpointURI endpointUri;

    private UMOWorkManager workManager;

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#create(org.mule.umo.UMOSession, org.mule.umo.endpoint.UMOEndpoint, org.mule.umo.UMOExceptionStrategy)
     */
    public synchronized void create(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint) throws InitialisationException {
        setConnector(connector);
        setComponent(component);
        setEndpoint(endpoint);
        endpointUri = endpoint.getEndpointURI();
        model = MuleManager.getInstance().getModel();
        if (connector instanceof AbstractConnector) {
            ThreadingProfile tp = ((AbstractConnector) connector).getReceiverThreadingProfile();
            if (serverSide) {
                tp.setThreadPriority(Thread.NORM_PRIORITY + 2);
            }
            workManager = tp.createWorkManager(connector.getName() + ".receiver");
            try {
                workManager.start();
            } catch (UMOException e) {
                throw new InitialisationException(e, this);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#getEndpointName()
     */
    public UMOEndpoint getEndpoint() {
        return endpoint;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#getExceptionListener()
     */
    public void handleException(Exception exception) {
        connector.getExceptionListener().exceptionThrown(exception);
        String propName = ExceptionHelper.getErrorCodePropertyName(connector.getProtocol());
        //If we dont find a error code property we can assume there are not
        //error code mappings for this connector
        UMOMessage message = RequestContext.getEvent().getMessage();
        if (propName != null && message != null) {
            String code = ExceptionHelper.getErrorMapping(connector.getProtocol(), exception.getClass());
            if (logger.isDebugEnabled()) logger.debug("Setting error code for: " + connector.getProtocol() + ", " + propName + "=" + code);
            message.setProperty(propName, code);
        }
    }

    public UMOConnector getConnector() {
        return connector;
    }

    public void setConnector(UMOConnector connector) {
        if (connector != null) {
            this.connector = connector;
        } else {
            throw new IllegalArgumentException("Connector cannot be null");
        }
    }

    public UMOComponent getComponent() {
        return component;
    }

    public final UMOMessage routeMessage(UMOMessage message) throws UMOException {
        return routeMessage(message, (endpoint.isSynchronous() || TransactionCoordination.getInstance().getTransaction()!=null));
    }

    public final UMOMessage routeMessage(UMOMessage message, boolean synchronous) throws UMOException {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, null);
    }

    public final UMOMessage routeMessage(UMOMessage message, UMOTransaction trans, boolean synchronous) throws UMOException {
        return routeMessage(message, trans, synchronous, null);
    }

    public final UMOMessage routeMessage(UMOMessage message, OutputStream outputStream) throws UMOException {
        return routeMessage(message, endpoint.isSynchronous(), outputStream);
    }

    public final UMOMessage routeMessage(UMOMessage message, boolean synchronous, OutputStream outputStream) throws UMOException {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, outputStream);
    }

    public final UMOMessage routeMessage(UMOMessage message, UMOTransaction trans, boolean synchronous, OutputStream outputStream) throws UMOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Message Received from: " + endpoint.getEndpointURI());
            logger.debug(message);
        }
        if (logger.isTraceEnabled()) {
            try {
                logger.trace("Message Payload: \n" + message.getPayloadAsString());
            } catch (Exception e) {
                //ignore
            }
        }

        ResponseOutputStream ros = null;
        if (outputStream != null) {
            if (outputStream instanceof ResponseOutputStream) {
                ros = (ResponseOutputStream) outputStream;
            } else {
                ros = new ResponseOutputStream(outputStream);
            }
        }

        //Apply the endpoint filter if one is configured
        if (endpoint.getFilter() != null) {
            if (!endpoint.getFilter().accept(message)) {
                handleUnacceptedFilter(message);
                return null;
            }
        }
        UMOSession session = new MuleSession(component, trans);
        UMOEvent muleEvent = new MuleEvent(message, endpoint, session, synchronous, ros);
        RequestContext.setEvent(muleEvent);

        //Apply Security filter if one is set
        if (endpoint.getSecurityFilter() != null) {
            try {
                endpoint.getSecurityFilter().authenticate(muleEvent);
            } catch (SecurityException e) {
                logger.warn("Request was made but was not authenticated: " + e.getMessage(), e);
                handleException(e);
                return message;
            }
        }
        //the security filter may update the payload so we need to get the
        //latest event again
        muleEvent = RequestContext.getEvent();

        UMOMessage resultMessage = null;
        //This is a replyTo event for a current request
        if (UMOEndpoint.ENDPOINT_TYPE_RESPONSE.equals(endpoint.getType())) {
            component.getDescriptor().getResponseRouter().route(muleEvent);
            return null;
        } else {
            resultMessage = component.getDescriptor().getInboundRouter().route(muleEvent);
        }
        return resultMessage;
    }

    protected UMOMessage handleUnacceptedFilter(UMOMessage message) {
        String messageId = null;
        try {
            messageId = message.getUniqueId();
        } catch (UniqueIdNotSupportedException e) {
            messageId = "'no unique id'";
        }
        logger.warn("Message " + messageId + " failed to pass filter on endpoint: " + endpoint + ". Message is being ignored");

        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#setEndpoint(org.mule.umo.endpoint.UMOEndpoint)
     */
    public void setEndpoint(UMOEndpoint endpoint) {
        if (endpoint == null) throw new IllegalArgumentException("Provider cannot be null");
        if (endpoint.getFilter() != null && !allowFilter(endpoint.getFilter())) {
            throw new UnsupportedOperationException("Message filter: " + endpoint.getFilter().getClass().getName() + " is not supported by this connector: " + connector.getClass().getName());
        }
        this.endpoint = endpoint;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#setSession(org.mule.umo.UMOSession)
     */
    public void setComponent(UMOComponent component) {
        if (component == null) throw new IllegalArgumentException("Component cannot be null");
        this.component = component;
    }

    public final void dispose() {
        disposing.set(true);
        workManager.dispose();
        doDispose();
    }

    /**
     * Template method to dispose any resources associated with this receiver.  There
     * is not need to dispose the connector as this is already done by the framework
     */
    protected void doDispose() {
    }

    public UMOEndpointURI getEndpointURI() {
        return endpointUri;
    }

    boolean acceptMessage(Object message) {
        if (endpoint.getFilter() != null) {
            return endpoint.getFilter().accept(message);
        } else {
            return true;
        }
    }

    protected boolean allowFilter(UMOFilter filter) throws UnsupportedOperationException {
        //By default we  support all filters on endpoints
        return true;
    }

    public boolean isServerSide() {
        return serverSide;
    }

    public void setServerSide(boolean serverSide) {
        this.serverSide = serverSide;
    }

    protected UMOWorkManager getWorkManager() {
        return workManager;
    }

    protected void setWorkManager(UMOWorkManager workManager) {
        this.workManager = workManager;
    }
}
