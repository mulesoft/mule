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
package org.mule.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.config.MuleProperties;
import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransaction;
import org.mule.umo.endpoint.EndpointNotFoundException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.util.UUID;

/**
 * <code>MuleSession</code>  manages the interaction and distribution of events for Mule UMOs.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public final class MuleSession implements UMOSession
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(MuleSession.class);

    /**
     * The Mule Component associated with the component
     */
    private UMOComponent component = null;

    /**
     * Determines if the component is valid
     */
    private boolean valid = true;

    private UMOTransaction transaction;

    private String id;

    public MuleSession(UMOTransaction transaction)
    {
        this.transaction = transaction;
        this.id = new UUID().getUUID();
    }

    public MuleSession(UMOComponent component, UMOTransaction transaction)
    {
        this(transaction);
        if (component == null)
        {
            throw new IllegalArgumentException("Component cannot be null");
        }
        this.component = component;
    }

    public void dispatchEvent(UMOMessage message) throws UMOException
    {
        UMOOutboundMessageRouter router = component.getDescriptor().getOutboundRouter();
        if(router==null) {
            throw new EndpointNotFoundException("There is no outbound router configured on componennt: " + component.getDescriptor().getName());
        }
        router.route(message, this, false);
    }

    public void dispatchEvent(UMOMessage message, String endpointName) throws UMOException
    {
        dispatchEvent(message, MuleManager.getInstance().lookupEndpoint(endpointName));
    }

    public void dispatchEvent(UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        logger.debug("Session has received asynchronous event on: " + endpoint);
        if (endpoint == null && component == null)
        {
            throw new EndpointNotFoundException("Cannot build null endpoint for session. Session component is also null");
        } else if (endpoint==null) {
            endpoint = component.getDescriptor().getOutboundEndpoint();
        }
        UMOEvent event = createOutboundEvent(message, endpoint, RequestContext.getEvent());
        dispatchEvent(event);
    }

    public UMOMessage sendEvent(UMOMessage message, String endpointName) throws UMOException
    {
        return sendEvent(message, MuleManager.getInstance().lookupEndpoint(endpointName));
    }

    public UMOMessage sendEvent(UMOMessage message) throws UMOException
    {
        UMOOutboundMessageRouter router = component.getDescriptor().getOutboundRouter();
        if(router==null) {
            throw new EndpointNotFoundException("There is no outbound router configured on componennt: " + component.getDescriptor().getName());
        }
        return router.route(message, this, true);
    }

    public UMOMessage sendEvent(UMOMessage message, UMOEndpoint endpoint) throws UMOException
    {
        logger.debug("Session has received synchronous event on: " + endpoint);
        if (endpoint == null && component == null)
        {
            throw new EndpointNotFoundException("Cannot build null endpoint for session. Session component is also null");
        } else if (endpoint == null) {
            endpoint = component.getDescriptor().getOutboundEndpoint();
        }

        UMOEvent event = createOutboundEvent(message, endpoint, RequestContext.getEvent());

        return sendEvent(event);
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#dispatchEvent(org.mule.umo.UMOEvent)
     */
    public synchronized void dispatchEvent(UMOEvent event) throws UMOException
    {
        if(event.getEndpoint().canSend()) {
            try
            {
                logger.debug("dispatching event: " + event);
                UMOMessageDispatcher dispatcher = event.getEndpoint().getConnector().getDispatcher(event.getEndpoint().getEndpointURI().getAddress());
                dispatcher.dispatch(event);

            } catch (Exception e)
            {
                throw new MuleException("Failed to send event through Connector dispatcher: " + e.getMessage(), e);
            }

        } else if(component!=null){
            logger.debug("dispatching event to component: " + component.getDescriptor().getName() + " event is: " + event);
            component.dispatchEvent(event);

        } else {
            throw new MuleException("Cannot dispatch event, endpoint is a receiver and there is no current compoenent");
        }
    }

    public String getId()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#sendEvent(org.mule.umo.UMOEvent)
     */
    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        String timeout = (String)event.removeProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY);
        if(timeout!=null) {
            event.setTimeout(Integer.parseInt(timeout));
        }

        if(event.getEndpoint().canSend()) {
            try
            {
                logger.debug("sending event: " + event);
                UMOMessageDispatcher dispatcher = event.getEndpoint().getConnector().getDispatcher(event.getEndpoint().getEndpointURI().getAddress());
                return dispatcher.send(event);

            } catch (UMOException e) {
                throw e;
            }
            catch (Exception e)
            {
                throw new MuleException("Failed to send event through Connector dispatcher: " + e.getMessage(), e);
            }

        } else if(component!=null){
            logger.debug("dispatching event to component: " + component.getDescriptor().getName() + " event is: " + event);
            return component.sendEvent(event);

        } else {
            throw new MuleException("Cannot send event, endpoint is a receiver and there is no current compoenent");
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#isValid()
     */
    public boolean isValid()
    {
        return valid;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#setValid(boolean)
     */
    public void setValid(boolean value)
    {
        valid = value;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#receiveEvent(org.mule.umo.endpoint.UMOEndpoint, long, org.mule.umo.UMOEvent)
     */
    public UMOMessage receiveEvent(String endpointName, long timeout) throws UMOException
    {
        UMOEndpoint endpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointName, UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        return receiveEvent(endpoint, timeout);
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#receiveEvent(org.mule.umo.endpoint.UMOEndpoint, long, org.mule.umo.UMOEvent)
     */
    public UMOMessage receiveEvent(UMOEndpoint endpoint, long timeout) throws UMOException
    {
        try
        {
            UMOMessageDispatcher dispatcher = endpoint.getConnector().getDispatcher(endpoint.getEndpointURI().getAddress());
            return dispatcher.receive(endpoint.getEndpointURI(), timeout);
        } catch (Exception e)
        {
            throw new MuleException("Failed to receive event: " + e.getMessage(), e);
        }
    }

    public UMOEvent createOutboundEvent(UMOMessage message, UMOEndpoint endpoint, UMOEvent previousEvent) throws UMOException
    {
        logger.debug("Creating event with data: " + message.getPayload().getClass().getName() + ", for endpoint: " + endpoint.toString());

        if (endpoint == null)
        {
            throw new MuleException("When creating an event the proivder cannot be null");
        }
        if (message == null)
        {
            throw new MuleException("When creating an event the data and/or the properties must be set");
        }
        //Use default transformer if none is set
        if(endpoint.getTransformer() == null)
        {
            if(endpoint.getConnector() instanceof AbstractConnector) {
                endpoint.setTransformer(((AbstractConnector)endpoint.getConnector()).getDefaultOutboundTransformer());
                logger.debug("Using default connector outbound transformer: " + endpoint.getTransformer());
             }
        }
        try
        {
            UMOEvent event;
            if(previousEvent!=null) {
                event = new MuleEvent(message, endpoint, component, previousEvent);
            } else {
                event = new MuleEvent(message, endpoint, this, false, null);
            }
            return event;
        }
        catch (Exception e)
        {
            throw new MuleException("Failed to create event using endpoint: " + endpoint.getConnector().getClass().getName(),
                    e);
        }
    }

    /**
     * @return Returns the component.
     */
    public UMOComponent getComponent()
    {
        return component;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOSession#beginTransaction()
     */
    public UMOTransaction getTransaction()
    {
        return transaction;
    }

    void setComponent(UMOComponent component) {
        this.component = component;
    }
}
