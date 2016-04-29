/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.consumer;


import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.connector.ConnectorOperationLocator;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.Preconditions;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.runtime.module.http.api.requester.HttpRequesterConfig;
import org.mule.runtime.module.ws.security.WSSecurity;

public class WSConsumerConfig implements MuleContextAware
{

    private MuleContext muleContext;
    private String name;
    private String wsdlLocation;
    private String service;
    private String port;
    private String serviceAddress;
    private Connector connector;
    private HttpRequesterConfig connectorConfig;
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

        return createHttpRequester();
    }

    private MessageProcessor createHttpRequester() throws MuleException
    {
        return new MessageProcessor()
        {
            private HttpRequestOptions requestOptions;

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                ConnectorOperationLocator connectorOperationLocator = muleContext.getRegistry().get(OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR);
                MessageProcessor messageProcessor = connectorOperationLocator.locateConnectorOperation(serviceAddress, getRequestOptions(), REQUEST_RESPONSE);
                return messageProcessor.process(event);
            }

            private HttpRequestOptions getRequestOptions()
            {
                if (requestOptions == null)
                {
                    final HttpRequestOptionsBuilder builder = newOptions().method(POST.name()).disableStatusCodeValidation().disableParseResponse();
                    if (connectorConfig != null)
                    {
                        builder.requestConfig(connectorConfig);
                    }
                    requestOptions = builder.build();
                }
                return requestOptions;
            }
        };
    }

    private boolean isHttp()
    {
        return serviceAddress.startsWith(HttpConstants.Protocols.HTTP.getScheme());
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

    public HttpRequesterConfig getConnectorConfig()
    {
        return connectorConfig;
    }

    public void setConnectorConfig(HttpRequesterConfig connectorConfig)
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
