/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.connector;

import static org.mule.runtime.core.PropertyScope.OUTBOUND;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.EndpointFactory;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transport.service.TransportFactory;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultReplyToHandler</code> is responsible for processing a message
 * replyTo header.
 */

public class DefaultReplyToHandler implements ReplyToHandler, Serializable, DeserializationPostInitialisable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    private static final int CACHE_MAX_SIZE = 1000;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected transient MuleContext muleContext;
    private transient Map<String, Object> serializedData = null;

    protected transient Connector connector;
    private transient LoadingCache<String, OutboundEndpoint> endpointCache;

    public DefaultReplyToHandler(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        endpointCache = buildCache(muleContext);
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("sending reply to: " + replyTo);
        }
        String replyToEndpoint = replyTo.toString();


        // make sure remove the replyTo property as not cause a a forever
        // replyto loop
        returnMessage.removeProperty(MuleProperties.MULE_REPLY_TO_PROPERTY, OUTBOUND);
        event.removeFlowVariable(MuleProperties.MULE_REPLY_TO_PROPERTY);

        // MULE-4617. This is fixed with MULE-4620, but lets remove this property
        // anyway as it should never be true from a replyTo dispatch
        returnMessage.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, OUTBOUND);
        event.removeFlowVariable(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);

        // Create a new copy of the message so that response MessageProcessors don't end up screwing up the reply
        returnMessage = new DefaultMuleMessage(returnMessage.getPayload(), returnMessage, muleContext);

        // Create the replyTo event asynchronous
        MuleEvent replyToEvent = new DefaultMuleEvent(returnMessage, event);

        //TODO See MULE-9307 - re-add behaviour to process reply to destination dispatching with new connectors
        // get the endpoint for this url
        OutboundEndpoint endpoint = getEndpoint(event, replyToEndpoint);

        // carry over properties
        List<String> responseProperties = endpoint.getResponseProperties();
        for (String propertyName : responseProperties)
        {
            Object propertyValue = event.getMessage().getInboundProperty(propertyName);
            if (propertyValue != null)
            {
                replyToEvent.getMessage().setOutboundProperty(propertyName, propertyValue);
            }
        }

        // dispatch the event
        try
        {
            endpoint.process(replyToEvent);
            if (logger.isInfoEnabled())
            {
                logger.info("reply to sent: " + endpoint);
            }
        }
        catch (Exception e)
        {
            throw new DispatchException(CoreMessages.failedToDispatchToReplyto(endpoint),
                    replyToEvent, endpoint, e);
        }
    }

    @Override
    public void processExceptionReplyTo(MessagingException exception, Object replyTo)
    {
       // DefaultReplyToHandler does not send a reply message when an exception errors, this is rather handled by
       // using an exception strategy.
    }

    /**
     * @deprecated Transport infrastructure is deprecated.
     */
    @Deprecated
    protected synchronized OutboundEndpoint getEndpoint(MuleEvent event, String endpointUri) throws MuleException
    {
        try
        {
            return endpointCache.get(endpointUri);
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    public void initAfterDeserialisation(MuleContext context) throws MuleException
    {
        // this method can be called even on objects that were not serialized. In this case,
        // the temporary holder for serialized data is not initialized and we can just return
        if (serializedData == null)
        {
            return;
        }
        this.muleContext = context;

        logger = LogFactory.getLog(getClass());
        connector = findConnector();
        serializedData = null;
        endpointCache = buildCache(muleContext);
    }

    public Connector getConnector()
    {
        return connector;
    }

    protected Connector findConnector()
    {
        String connectorName = (String) serializedData.get("connectorName");
        String connectorType = (String) serializedData.get("connectorType");
        Connector found = null;

        if (connectorName != null)
        {
            found = muleContext.getRegistry().get(connectorName);
        }
        else if (connectorType != null)
        {
            found = new TransportFactory(muleContext).getDefaultConnectorByProtocol(connectorType);
        }
        return found;
    }

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();

        String connectorName = null;
        String connectorType = null;

        //Can be null if service call originates from MuleClient
        if (serializedData != null)
        {
            connectorName = (String) serializedData.get("connectorName");
            connectorType = (String) serializedData.get("connectorType");
        }
        else
        {
            //TODO See MULE-9307 - add behaviour to store config name to be used for reply to destination
        }
        out.writeObject(connectorName);
        out.writeObject(connectorType);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        serializedData = new HashMap<String, Object>();

        serializedData.put("connectorName", in.readObject());
        serializedData.put("connectorType", in.readObject());
    }

    private LoadingCache<String, OutboundEndpoint> buildCache(final MuleContext muleContext)
    {
        return CacheBuilder.newBuilder()
                           .maximumSize(CACHE_MAX_SIZE)
                           .<String, OutboundEndpoint> build(buildCacheLoader(muleContext));
    }

    private CacheLoader buildCacheLoader(final MuleContext muleContext)
    {
        return new CacheLoader<String, OutboundEndpoint>()
        {
            @Override
            public OutboundEndpoint load(String key) throws Exception
            {
                EndpointFactory endpointFactory = muleContext.getEndpointFactory();
                EndpointBuilder endpointBuilder = endpointFactory.getEndpointBuilder(key);
                return endpointFactory.getOutboundEndpoint(endpointBuilder);
            }
        };
    }

}
