/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;


import static javax.wsdl.WSDLException.OTHER_ERROR;
import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.api.config.MuleProperties.OBJECT_CONNECTOR_MESSAGE_PROCESSOR_LOCATOR;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.connector.ConnectorOperationLocator;
import org.mule.api.context.MuleContextAware;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.api.client.HttpRequestOptionsBuilder;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.internal.config.HttpConfiguration;
import org.mule.module.ws.security.WSSecurity;
import org.mule.transport.http.HttpConnector;
import org.mule.util.Preconditions;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

public class WSConsumerConfig implements MuleContextAware
{

    private MuleContext muleContext;
    private String name;
    private String wsdlLocation;
    private String service;
    private String port;
    private String serviceAddress;
    private boolean useConnectorToRetrieveWsdl;
    private Connector connector;
    private HttpRequesterConfig connectorConfig;
    private WSSecurity security;
    private volatile Definition wsdlDefinition;
    private volatile MuleWSDLLocator wsdlLocator;
    private Map<String, String> operationToBodyCache = new HashMap<>();

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
        if (HttpConfiguration.useTransportForUris(muleContext))
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

    public boolean isUseConnectorToRetrieveWsdl()
    {
        return useConnectorToRetrieveWsdl;
    }

    public void setUseConnectorToRetrieveWsdl(boolean useConnectorToRetrieveWsdl)
    {
        this.useConnectorToRetrieveWsdl = useConnectorToRetrieveWsdl;
    }

    public Definition getWsdlDefinition() throws Exception
    {
        if (wsdlDefinition == null)
        {
            synchronized (this)
            {
                if (wsdlDefinition == null)
                {
                    initializeWSDLLocator();
                    WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
                    wsdlDefinition = wsdlReader.readWSDL(wsdlLocator);
                }
            }
        }

        return wsdlDefinition;

    }

    protected void initializeWSDLLocator() throws WSDLException
    {
        try
        {
            MuleWSDLLocatorConfig locatorConfig = createWSDLLocator(connectorConfig, wsdlLocation);
            wsdlLocator = new MuleWSDLLocator(locatorConfig);
        }
        catch (Exception e)
        {
            throw new WSDLException(OTHER_ERROR, e.getMessage(), e);
        }
    }


    private MuleWSDLLocatorConfig createWSDLLocator(HttpRequesterConfig httpRequesterConfig, String url) throws Exception
    {
        MuleWSDLLocatorConfig locatorConfig = null;

        if (httpRequesterConfig == null && useConnectorToRetrieveWsdl)
        {
            throw new Exception("The useConnectorToRetrieveWsdl option requires connectorConfig to work");
        }

        if (httpRequesterConfig == null)
        {

            locatorConfig = new MuleWSDLLocatorConfig.Builder()
                                                               .setBaseURI(url)
                                                               .setContext(muleContext)
                                                               .build();
        }
        else
        {
            locatorConfig = new MuleWSDLLocatorConfig.Builder()
                                                               .setBaseURI(url)
                                                               .setTlsContextFactory(httpRequesterConfig.getTlsContext())
                                                               .setContext(muleContext)
                                                               .setUseConnectorToRetrieveWsdl(useConnectorToRetrieveWsdl)
                                                               .setProxyConfig(httpRequesterConfig.getProxyConfig())
                                                               .build();

        }
        return locatorConfig;
    }

    public MuleWSDLLocator getWsdlLocator() throws WSDLException
    {
        if (wsdlLocator == null)
        {
            synchronized (this)
            {
                initializeWSDLLocator();
            }

        }

        return wsdlLocator;
    }

    /**
     * Returns the cached request body for the given operation if present in the config, or generates a new one otherwise.
     */
    public String getRequestBody(Definition wsdlDefinition, BindingOperation bindingOperation) throws WSDLException
    {
        synchronized (this)
        {
            String body;
            String operationName = bindingOperation.getName();
            if (operationToBodyCache.containsKey(operationName))
            {
                body = operationToBodyCache.get(operationName);
            }
            else
            {
                RequestBodyGenerator requestBodyGenerator = new RequestBodyGenerator(wsdlDefinition, getWsdlLocator());
                body = requestBodyGenerator.generateRequestBody(bindingOperation);
                operationToBodyCache.put(operationName, body);
            }
            return body;
        }
    }
}
