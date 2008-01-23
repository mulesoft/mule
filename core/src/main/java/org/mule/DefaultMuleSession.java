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

import org.mule.api.MuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.component.Component;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointNotFoundException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.security.SecurityContext;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.ReceiveException;
import org.mule.api.transport.SessionHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerUtils;
import org.mule.transport.AbstractConnector;
import org.mule.util.UUID;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleSession</code> manages the interaction and distribution of events for
 * Mule UMOs.
 */

public final class DefaultMuleSession implements MuleSession
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3380926585676521866L;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(DefaultMuleSession.class);

    /**
     * The Mule component associated with the session
     */
    private Component component = null;

    /**
     * Determines if the component is valid
     */
    private boolean valid = true;

    private String id;

    private SecurityContext securityContext;

    private Map properties = null;

    public DefaultMuleSession(Component component)
    {
        properties = new HashMap();
        id = UUID.getUUID();
        this.component = component;
    }

    public DefaultMuleSession(MuleMessage message, SessionHandler requestSessionHandler, Component component)
        throws MuleException
    {
        this(message, requestSessionHandler);
        if (component == null)
        {
            throw new IllegalArgumentException(
                CoreMessages.propertiesNotSet("component").toString());
        }
        this.component = component;
    }

    public DefaultMuleSession(MuleMessage message, SessionHandler requestSessionHandler) throws MuleException
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

    public void dispatchEvent(MuleMessage message) throws MuleException
    {
        if (component == null)
        {
            throw new IllegalStateException(CoreMessages.objectIsNull("Component").getMessage());
        }

        OutboundRouterCollection router = component.getOutboundRouter();
        if (router == null)
        {
            throw new EndpointNotFoundException(
                CoreMessages.noOutboundRouterSetOn(component.getName()));
        }
        router.route(message, this, false);
    }

    public void dispatchEvent(MuleMessage message, String endpointName) throws MuleException
    {
        dispatchEvent(message, RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName));
    }
 
    public void dispatchEvent(MuleMessage message, ImmutableEndpoint endpoint) throws MuleException
    {
        if (endpoint == null)
        {
            logger.warn("Endpoint argument is null, using outbound router to determine endpoint.");
            dispatchEvent(message);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("MuleSession has received asynchronous event on: " + endpoint);
        }

        MuleEvent event = createOutboundEvent(message, endpoint, null);

        dispatchEvent(event);

        processResponse(event.getMessage());
    }

    public MuleMessage sendEvent(MuleMessage message, String endpointName) throws MuleException
    {
        return sendEvent(message, RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName));
    }

    public MuleMessage sendEvent(MuleMessage message) throws MuleException
    {
        if (component == null)
        {
            throw new IllegalStateException(CoreMessages.objectIsNull("Component").getMessage());
        }
        OutboundRouterCollection router = component.getOutboundRouter();
        if (router == null)
        {
            throw new EndpointNotFoundException(
                CoreMessages.noOutboundRouterSetOn(component.getName()));
        }
        MuleMessage result = router.route(message, this, true);
        if (result != null)
        {
            processResponse(result);
        }

        return result;
    }

    public MuleMessage sendEvent(MuleMessage message, ImmutableEndpoint endpoint) throws MuleException
    {
        if (endpoint == null)
        {
            logger.warn("Endpoint argument is null, using outbound router to determine endpoint.");
            return sendEvent(message);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("MuleSession has received synchronous event on endpoint: " + endpoint);
        }

        MuleEvent event = createOutboundEvent(message, endpoint, null);
        MuleMessage result = sendEvent(event);

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
     * @see org.mule.api.MuleSession#dispatchEvent(org.mule.api.MuleEvent)
     */
    public void dispatchEvent(MuleEvent event) throws MuleException
    {
        if (event.getEndpoint().canSend())
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("dispatching event: " + event);
                }

                Connector connector = event.getEndpoint().getConnector();

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
     * @see org.mule.api.MuleSession#sendEvent(org.mule.api.MuleEvent)
     */
    // TODO This method is practically the same as dispatchEvent(MuleEvent event),
    // so we could use some refactoring here.
    public MuleMessage sendEvent(MuleEvent event) throws MuleException
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

                Connector connector = event.getEndpoint().getConnector();

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

                MuleMessage response = event.getEndpoint().send(event);
                // See MULE-2692
                response = OptimizedRequestContext.unsafeRewriteEvent(response);
                processResponse(response);
                return response;
            }
            catch (MuleException e)
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
    protected void processResponse(MuleMessage response)
    {
        if (response == null)
        {
            return;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.api.MuleSession#isValid()
     */
    public boolean isValid()
    {
        return valid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.api.MuleSession#setValid(boolean)
     */
    public void setValid(boolean value)
    {
        valid = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.api.MuleSession#receiveEvent(org.mule.api.endpoint.Endpoint,
     *      long, org.mule.api.MuleEvent)
     */
    public MuleMessage receiveEvent(String endpointName, long timeout) throws MuleException
    {
        ImmutableEndpoint endpoint = RegistryContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(endpointName);
        return receiveEvent(endpoint, timeout);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.api.MuleSession#receiveEvent(org.mule.api.endpoint.Endpoint,
     *      long, org.mule.api.MuleEvent)
     */
    public MuleMessage receiveEvent(ImmutableEndpoint endpoint, long timeout) throws MuleException
    {
        try
        {
            return endpoint.request(timeout);
        }
        catch (Exception e)
        {
            throw new ReceiveException(endpoint, timeout, e);
        }
    }

    public MuleEvent createOutboundEvent(MuleMessage message,
                                        ImmutableEndpoint endpoint,
                                        MuleEvent previousEvent) throws MuleException
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
            MuleEvent event;
            if (previousEvent != null)
            {
                event = new DefaultMuleEvent(message, endpoint, component, previousEvent);
            }
            else
            {
                event = new DefaultMuleEvent(message, endpoint, this, false, null);
            }
            return event;
        }
        catch (Exception e)
        {
            throw new DispatchException(
                CoreMessages.failedToCreate("MuleEvent"), message, endpoint, e);
        }
    }

    /**
     * @return Returns the component.
     */
    public Component getComponent()
    {
        return component;
    }

    void setComponent(Component component)
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
    public void setSecurityContext(SecurityContext context)
    {
        securityContext = context;
    }

    /**
     * The security context for this session. If not null outbound, inbound and/or
     * method invocations will be authenticated using this context
     * 
     * @return the context for this session or null if the request is not secure.
     */
    public SecurityContext getSecurityContext()
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
