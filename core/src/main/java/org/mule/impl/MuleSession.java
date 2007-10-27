/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.AbstractConnector;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.EndpointNotFoundException;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.ReceiveException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.util.UUID;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleSession</code> manages the interaction and distribution of events for
 * Mule UMOs.
 */

public final class MuleSession implements UMOSession
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3380926585676521866L;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(MuleSession.class);

    /**
     * The Mule component associated with the session
     */
    private UMOComponent component = null;

    /**
     * Determines if the component is valid
     */
    private boolean valid = true;

    private String id;

    private UMOSecurityContext securityContext;

    private Map properties = null;

    public MuleSession(UMOComponent component)
    {
        properties = new HashMap();
        id = UUID.getUUID();
        this.component = component;
    }

    public MuleSession(UMOMessage message, UMOSessionHandler requestSessionHandler, UMOComponent component)
        throws UMOException
    {
        this(message, requestSessionHandler);
        if (component == null)
        {
            throw new IllegalArgumentException(
                CoreMessages.propertiesNotSet("component").toString());
        }
        this.component = component;
    }

    public MuleSession(UMOMessage message, UMOSessionHandler requestSessionHandler) throws UMOException
    {

        if (requestSessionHandler == null)
        {
            throw new IllegalArgumentException(
                CoreMessages.propertiesNotSet("requestSessionHandler").toString());
        }

        if (message == null)
        {
            throw new IllegalArgumentException(
                CoreMessages.propertiesNotSet("message").toString());
        }

        properties = new HashMap();
        requestSessionHandler.retrieveSessionInfoFromMessage(message, this);
        id = (String) getProperty(requestSessionHandler.getSessionIDKey());
        if (id == null)
        {
            id = UUID.getUUID();
            if (logger.isDebugEnabled())
            {
                logger.debug("There is no session id on the request using key: "
                             + requestSessionHandler.getSessionIDKey() + ". Generating new session id: " + id);
            }
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Got session with id: " + id);
        }
    }

    public void dispatchEvent(UMOMessage message) throws UMOException
    {
        if (component == null)
        {
            throw new IllegalStateException(CoreMessages.objectIsNull("Component").getMessage());
        }

        UMOOutboundRouterCollection router = component.getOutboundRouter();
        if (router == null)
        {
            throw new EndpointNotFoundException(
                CoreMessages.noOutboundRouterSetOn(component.getName()));
        }
        router.route(message, this, false);
    }

    public void dispatchEvent(UMOMessage message, String endpointName) throws UMOException
    {
        dispatchEvent(message, RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName,
            MuleServer.getManagementContext()));
    }
 
    public void dispatchEvent(UMOMessage message, UMOImmutableEndpoint endpoint) throws UMOException
    {
        if (endpoint == null)
        {
            logger.warn("Endpoint argument is null, using outbound router to determine endpoint.");
            dispatchEvent(message);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Session has received asynchronous event on: " + endpoint);
        }

        UMOEvent event = createOutboundEvent(message, endpoint, null);

        dispatchEvent(event);

        processResponse(event.getMessage());
    }

    public UMOMessage sendEvent(UMOMessage message, String endpointName) throws UMOException
    {
        return sendEvent(message, RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointName, MuleServer.getManagementContext()));
    }

    public UMOMessage sendEvent(UMOMessage message) throws UMOException
    {
        if (component == null)
        {
            throw new IllegalStateException(CoreMessages.objectIsNull("Component").getMessage());
        }
        UMOOutboundRouterCollection router = component.getOutboundRouter();
        if (router == null)
        {
            throw new EndpointNotFoundException(
                CoreMessages.noOutboundRouterSetOn(component.getName()));
        }
        UMOMessage result = router.route(message, this, true);
        if (result != null)
        {
            processResponse(result);
        }

        return result;
    }

    public UMOMessage sendEvent(UMOMessage message, UMOImmutableEndpoint endpoint) throws UMOException
    {
        if (endpoint == null)
        {
            logger.warn("Endpoint argument is null, using outbound router to determine endpoint.");
            return sendEvent(message);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Session has received synchronous event on endpoint: " + endpoint);
        }

        UMOEvent event = createOutboundEvent(message, endpoint, null);
        UMOMessage result = sendEvent(event);

        // Handles the situation where a response has been received via a remote
        // ReplyTo channel.
        if (endpoint.isRemoteSync() && TransformerUtils.isDefined(endpoint.getResponseTransformers()) && result != null)
        {
            result.applyTransformers(endpoint.getResponseTransformers());
        }

        if (result != null)
        {
            processResponse(result);
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOSession#dispatchEvent(org.mule.umo.UMOEvent)
     */
    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        if (event.getEndpoint().canSend())
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("dispatching event: " + event);
                }

                UMOConnector connector = event.getEndpoint().getConnector();

                if (connector instanceof AbstractConnector)
                {
                    ((AbstractConnector) connector).getSessionHandler().storeSessionInfoToMessage(this,
                        event.getMessage());
                }
                else
                {
                    // TODO in Mule 2.0 we'll flatten the Connector hierachy
                    logger.warn("A session handler could not be obtained, using  default");
                    new MuleSessionHandler().storeSessionInfoToMessage(this, event.getMessage());
                }

                event.getEndpoint().dispatch(event);
            }
            catch (Exception e)
            {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
        }
        else if (component != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("dispatching event to component: " + component.getName()
                             + ", event is: " + event);
            }
            component.dispatchEvent(event);
            processResponse(event.getMessage());
        }
        else
        {
            throw new DispatchException(CoreMessages.noComponentForEndpoint(), event.getMessage(),
                event.getEndpoint());
        }
    }

    public String getId()
    {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOSession#sendEvent(org.mule.umo.UMOEvent)
     */
    // TODO This method is practically the same as dispatchEvent(UMOEvent event),
    // so we could use some refactoring here.
    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        int timeout = event.getMessage().getIntProperty(MuleProperties.MULE_EVENT_TIMEOUT_PROPERTY, -1);
        if (timeout >= 0)
        {
            event.setTimeout(timeout);
        }

        if (event.getEndpoint().canSend())
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("sending event: " + event);
                }

                UMOConnector connector = event.getEndpoint().getConnector();

                if (connector instanceof AbstractConnector)
                {
                    ((AbstractConnector) connector).getSessionHandler().storeSessionInfoToMessage(this,
                        event.getMessage());
                }
                else
                {
                    // TODO in Mule 2.0 we'll flatten the Connector hierachy
                    logger.warn("A session handler could not be obtained, using default.");
                    new MuleSessionHandler().storeSessionInfoToMessage(this, event.getMessage());
                }

                UMOMessage response = event.getEndpoint().send(event);
                processResponse(response);
                return response;
            }
            catch (UMOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }

        }
        else if (component != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("sending event to component: " + component.getName()
                             + " event is: " + event);
            }
            return component.sendEvent(event);

        }
        else
        {
            throw new DispatchException(CoreMessages.noComponentForEndpoint(), event.getMessage(),
                event.getEndpoint());
        }
    }

    /**
     * Once an event has been processed we need to romove certain properties so that
     * they not propagated to the next request
     *
     * @param response The response from the previous request
     */
    protected void processResponse(UMOMessage response)
    {
        if (response == null)
        {
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOSession#isValid()
     */
    public boolean isValid()
    {
        return valid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOSession#setValid(boolean)
     */
    public void setValid(boolean value)
    {
        valid = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOSession#receiveEvent(org.mule.umo.endpoint.UMOEndpoint,
     *      long, org.mule.umo.UMOEvent)
     */
    public UMOMessage receiveEvent(String endpointName, long timeout) throws UMOException
    {
        UMOImmutableEndpoint endpoint = RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            endpointName, MuleServer.getManagementContext());
        return receiveEvent(endpoint, timeout);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.UMOSession#receiveEvent(org.mule.umo.endpoint.UMOEndpoint,
     *      long, org.mule.umo.UMOEvent)
     */
    public UMOMessage receiveEvent(UMOImmutableEndpoint endpoint, long timeout) throws UMOException
    {
        try
        {
            return endpoint.receive(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    public UMOEvent createOutboundEvent(UMOMessage message,
                                        UMOImmutableEndpoint endpoint,
                                        UMOEvent previousEvent) throws UMOException
    {
        if (endpoint == null)
        {
            throw new DispatchException(CoreMessages.objectIsNull("Outbound Endpoint"), message,
                endpoint);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Creating event with data: " + message.getPayload().getClass().getName()
                         + ", for endpoint: " + endpoint);
        }

        try
        {
            UMOEvent event;
            if (previousEvent != null)
            {
                event = new MuleEvent(message, endpoint, component, previousEvent);
            }
            else
            {
                event = new MuleEvent(message, endpoint, this, false, null);
            }
            return event;
        }
        catch (Exception e)
        {
            throw new DispatchException(
                CoreMessages.failedToCreate("Event"), message, endpoint, e);
        }
    }

    /**
     * @return Returns the component.
     */
    public UMOComponent getComponent()
    {
        return component;
    }

    void setComponent(UMOComponent component)
    {
        this.component = component;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @param context the context for this session or null if the request is not
     *            secure.
     */
    public void setSecurityContext(UMOSecurityContext context)
    {
        securityContext = context;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    public UMOSecurityContext getSecurityContext()
    {
        return securityContext;
    }

    /**
     * Will set a session level property. These will either be stored and retrieved
     * using the underlying transport mechanism of stored using a default mechanism
     * 
     * @param key the key for the object data being stored on the session
     * @param value the value of the session data
     */
    public void setProperty(Object key, Object value)
    {
        properties.put(key, value);
    }

    /**
     * Will retrieve a session level property.
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    public Object getProperty(Object key)
    {
        return properties.get(key);
    }

    /**
     * Will retrieve a session level property and remove it from the session
     * 
     * @param key the key for the object data being stored on the session
     * @return the value of the session data or null if the property does not exist
     */
    public Object removeProperty(Object key)
    {
        return properties.remove(key);
    }

    /**
     * Returns an iterater of property keys for the session properties on this
     * session
     * 
     * @return an iterater of property keys for the session properties on this
     *         session
     */
    public Iterator getPropertyNames()
    {
        return properties.keySet().iterator();
    }

}
