/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;


import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.module.http.api.requester.HttpRequesterBuilder;
import org.mule.module.http.internal.request.HttpRequestConfig;
import org.mule.module.http.internal.request.SuccessStatusCodeValidator;
import org.mule.module.ws.security.WSSecurity;
import org.mule.transport.http.HttpConnector;
import org.mule.util.Preconditions;
import org.mule.util.StringUtils;

public class WSConsumerConfig implements MuleContextAware
{

    private MuleContext muleContext;
    private String name;
    private String wsdlLocation;
    private String service;
    private String port;
    private String serviceAddress;
    private Connector connector;
    private HttpRequestConfig connectorConfig;
    private WSSecurity security;

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * Creates an outbound endpoint for the service address.
     */
    public MessageProcessor createOutboundMessageProcessor() throws MuleException
    {
        Preconditions.checkState(StringUtils.isNotEmpty(serviceAddress), "No serviceAddress provided in WS consumer config");

        if (connectorConfig != null && connector != null)
        {
            throw new ConfigurationException(CoreMessages.createStaticMessage("Cannot set both connector-config and connector-ref attributes. Set either one of them, or none for default behavior."));
        }

        if (useHttpModule())
        {
            return createHttpRequester();
        }
        else
        {
            return createOutboundEndpoint();
        }
    }


    private boolean useHttpModule()
    {
        if (connectorConfig != null)
        {
            return true;
        }
        if (!isHttp())
        {
            return false;
        }
        if (connector != null)
        {
            return false;
        }
        if (muleContext.getConfiguration().useHttpTransportByDefault())
        {
            return false;
        }
        return true;
    }

    private OutboundEndpoint createOutboundEndpoint() throws MuleException
    {
        EndpointBuilder builder = muleContext.getEndpointFactory().getEndpointBuilder(serviceAddress);

        if (connector != null)
        {
            String protocol = new MuleEndpointURI(serviceAddress, muleContext).getScheme();
            if (!connector.supportsProtocol(protocol))
            {
                throw new IllegalStateException(String.format("Connector %s does not support protocol: %s", connector.getName(), protocol));
            }

            builder.setConnector(connector);
        }

        return muleContext.getEndpointFactory().getOutboundEndpoint(builder);
    }

    private MessageProcessor createHttpRequester() throws MuleException
    {
        HttpRequesterBuilder requesterBuilder = new HttpRequesterBuilder(muleContext);

        requesterBuilder.setAddress(serviceAddress).setMethod("POST").setParseResponse(false);

        // Do not throw exception on invalid status code, let CXF process it.
        requesterBuilder.setResponseValidator(new SuccessStatusCodeValidator("0..599"));

        if (connectorConfig != null)
        {
            requesterBuilder.setConfig(connectorConfig);
        }

        return requesterBuilder.build();
    }

    private boolean isHttp()
    {
        return serviceAddress.startsWith(HttpConnector.HTTP);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getWsdlLocation()
    {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlLocation)
    {
        this.wsdlLocation = wsdlLocation;
    }

    public String getService()
    {
        return service;
    }

    public void setService(String service)
    {
        this.service = service;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    public String getServiceAddress()
    {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress)
    {
        this.serviceAddress = serviceAddress;
    }

    public Connector getConnector()
    {
        return connector;
    }

    public void setConnector(Connector connector)
    {
        this.connector = connector;
    }

    public HttpRequestConfig getConnectorConfig()
    {
        return connectorConfig;
    }

    public void setConnectorConfig(HttpRequestConfig connectorConfig)
    {
        this.connectorConfig = connectorConfig;
    }

    public WSSecurity getSecurity()
    {
        return security;
    }

    public void setSecurity(WSSecurity security)
    {
        this.security = security;
    }

}
