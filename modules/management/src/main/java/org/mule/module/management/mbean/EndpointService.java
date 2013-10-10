/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import org.mule.MessageExchangePattern;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ObjectNameHelper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The EndpointServiceMBean allows you to check the confiugration of an endpoint and
 * conect/disconnect endpoints manually.
 */
public class EndpointService implements EndpointServiceMBean
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private ImmutableEndpoint endpoint;
    private MessageReceiver receiver;
    private String name;
    private String componentName;

    public EndpointService(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        init();
    }

    public EndpointService(MessageReceiver receiver)
    {
        if (receiver == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("Receiver").getMessage());
        }
        this.endpoint = receiver.getEndpoint();
        this.receiver = receiver;
        this.componentName = receiver.getFlowConstruct().getName();
        init();
    }

    private void init()
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("Endpoint").getMessage());
        }
        if (receiver == null && endpoint instanceof InboundEndpoint)
        {
            throw new IllegalArgumentException(
                "Recevier is null for Endpoint MBean but the endpoint itself is a receiving endpoint");
        }

        name = new ObjectNameHelper(endpoint.getConnector().getMuleContext()).getEndpointName(endpoint.getEndpointURI());
    }

    public String getAddress()
    {
        return endpoint.getEndpointURI().getAddress();
    }

    public String getName()
    {
        return name;
    }

    public boolean isConnected()
    {
        return receiver == null || receiver.isConnected();
    }

    public void connect() throws Exception
    {
        if (receiver != null && !receiver.isConnected())
        {
            receiver.connect();
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Endpoint is already connected");
        }
    }

    public void disconnect() throws Exception
    {
        if (receiver != null && receiver.isConnected())
        {
            receiver.disconnect();
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Endpoint is already disconnected");
        }
    }

    public boolean isInbound()
    {
        return endpoint instanceof InboundEndpoint;
    }

    public boolean isOutbound()
    {
        return endpoint instanceof OutboundEndpoint;
    }

    public MessageExchangePattern getMessageExchangePattern()
    {
        return endpoint.getExchangePattern();
    }
    
    public String getComponentName()
    {
        return componentName;
    }

    public void setComponentName(String componentName)
    {
        this.componentName = componentName;
    }
}
