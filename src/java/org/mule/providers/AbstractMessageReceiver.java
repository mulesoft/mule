/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
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
import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.RequestContext;
import org.mule.impl.ResponseOutputStream;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.security.UMOSecurityException;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>AbstractMessageReceiver</code> provides common methods for all Message Receivers provided with Mule.
 * A message receiver enables an endpoint to receive a message from an external system.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageReceiver implements UMOMessageReceiver
{
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

    /** Stores the endpointUri that this receiver listens on.  This enpoint can be different
     * to the endpointUri in the endpoint stored on the receiver as endpoint endpointUri may get
     * rewritten if this endpointUri is a wildcard endpointUri such as jms.*
     */
    private UMOEndpointURI endpointUri;

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#create(org.mule.umo.UMOSession, org.mule.umo.endpoint.UMOEndpoint, org.mule.umo.UMOExceptionStrategy)
     */
    public synchronized void create(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint) throws InitialisationException
    {
        setConnector(connector);
        setComponent(component);
        setEndpoint(endpoint);
        endpointUri = endpoint.getEndpointURI();
        model = MuleManager.getInstance().getModel();
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#getEndpointName()
     */
    public UMOEndpoint getEndpoint()
    {
        return endpoint;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#getExceptionStrategy()
     */
    public void handleException(Object message, Throwable exception)
    {
        connector.getExceptionStrategy().handleException(message, exception);
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public void setConnector(UMOConnector connector)
    {
        if(connector != null) {
            this.connector = connector;
        } else {
            throw new IllegalArgumentException("Connector cannot be null");
        }
    }

    public UMOComponent getComponent()
    {
        return component;
    }

    public final UMOMessage routeMessage(UMOMessage message) throws UMOException
    {
        return routeMessage(message, endpoint.isSynchronous());
    }

    public final UMOMessage routeMessage(UMOMessage message, boolean synchronous) throws UMOException
    {
    	UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, null);
    }

    public final UMOMessage routeMessage(UMOMessage message, UMOTransaction trans, boolean synchronous) throws UMOException
    {
        return routeMessage(message, trans, synchronous, null);
    }

    public final UMOMessage routeMessage(UMOMessage message, OutputStream outputStream) throws UMOException
    {
        return routeMessage(message, endpoint.isSynchronous(), outputStream);
    }

    public final UMOMessage routeMessage(UMOMessage message, boolean synchronous, OutputStream outputStream) throws UMOException
    {
    	UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        return routeMessage(message, tx, tx != null || synchronous, outputStream);
    }

    public final  UMOMessage routeMessage(UMOMessage message, UMOTransaction trans, boolean synchronous, OutputStream outputStream) throws UMOException
    {
        if(logger.isDebugEnabled()) {
            logger.debug("Received message from: " + endpoint.getEndpointURI().getAddress());
            logger.debug("Payload is of type: " + message.getPayload().getClass().getName());
            StringBuffer buf = new StringBuffer();
            Map props = message.getProperties();
            for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry)iterator.next();
                buf.append("  ").append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            logger.debug("Message properties:\n" + buf.toString());
        }

        ResponseOutputStream ros=null;
        if(outputStream!=null) {
            if(outputStream instanceof ResponseOutputStream) {
                ros = (ResponseOutputStream)outputStream;
            } else {
                ros = new ResponseOutputStream(outputStream);
            }
        }

        //Apply the endpoint filter if one is configured
        if(endpoint.getFilter()!=null) {
            if(!endpoint.getFilter().accept(message)) {
                handleUnacceptedFilter(message);
                return null;
            }
        }
        UMOSession session = new MuleSession(component, trans);
        UMOEvent muleEvent = new MuleEvent(message, endpoint, session, synchronous, ros);
        RequestContext.setEvent(muleEvent);

        //Apple Security filter if one is set
        if(endpoint.getSecurityFilter()!=null) {
            try
            {
                endpoint.getSecurityFilter().authenticate(muleEvent);
            } catch (UMOSecurityException e)
            {
                logger.warn("Request was made but was not authenticated: " + e.getMessage(), e);
                return handleSecurtyException(e, muleEvent);
            }
        }

        UMOMessage resultMessage = null;
        //This is a replyTo event for a current request
        if(UMOEndpoint.ENDPOINT_TYPE_RESPONSE.equals(endpoint.getType())) {
            component.getDescriptor().getResponseRouter().route(muleEvent);
            return null;
        } else {
            resultMessage = component.getDescriptor().getInboundRouter().route(muleEvent);
        }
        return resultMessage;
    }

    protected UMOMessage handleSecurtyException(UMOSecurityException e, UMOEvent event) {
        UMOMessage m  = new MuleMessage(e.getMessage(), event.getProperties());
        //todo
        m.setErrorCode(100);
        return m;
    }

    protected UMOMessage handleUnacceptedFilter(UMOMessage message) {
        String messageId = null;
        try
        {
            messageId = message.getUniqueId();
        } catch (UniqueIdNotSupportedException e)
        {
            messageId = "'no unique id'";
        }
        logger.warn("Message " + messageId + " failed to pass filter on endpoint: " + endpoint + ". Message is being ignored");

        return null;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#setEndpoint(org.mule.umo.endpoint.UMOEndpoint)
     */
    public void setEndpoint(UMOEndpoint endpoint)
    {
        if (endpoint == null) throw new IllegalArgumentException("Provider cannot be null");
        if(endpoint.getFilter()!=null && !allowFilter(endpoint.getFilter()))
        {
            throw new UnsupportedOperationException("Message filter: " + endpoint.getFilter().getClass().getName() + " is not supported by this connector: " + connector.getClass().getName());
        }
        this.endpoint = endpoint;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.provider.UMOMessageReceiver#setSession(org.mule.umo.UMOSession)
     */
    public void setComponent(UMOComponent component)
    {
        if (component == null) throw new IllegalArgumentException("Component cannot be null");
        this.component = component;
    }

    public final void dispose() throws UMOException
    {
        disposing.set(true);
        doDispose();
    }
    /**
     * Template method to dispose any resources associated with this receiver.  There
     * is not need to dispose the connector as this is already done by the framework
     * @throws UMOException
     */
    protected void doDispose() throws UMOException
    {
    }

    public UMOEndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    boolean acceptMessage(Object message)
    {
        if(endpoint.getFilter() != null) {
            return endpoint.getFilter().accept(message);
        }else {
            return true;
        }
    }

    protected boolean allowFilter(UMOFilter filter) throws UnsupportedOperationException
    {
        //By default we  support all filters on endpoints
        return true;
    }

    public boolean isServerSide()
    {
        return serverSide;
    }

    public void setServerSide(boolean serverSide)
    {
        this.serverSide = serverSide;
    }
}
