/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.callback.HttpCallback;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.source.MessageSource;
import org.mule.api.transport.Connector;
import org.mule.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.construct.Flow;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerBuilder;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Default implementation of {@link org.mule.api.callback.HttpCallback}. This class
 * does not only start the http inbound endpoint that is able to receive the
 * callbacks, it also contains a processor chain to be invoked upon reception.
 */
public class DefaultHttpCallback implements HttpCallback
{

    private final static Logger LOGGER = Logger.getLogger(DefaultHttpCallback.class);
    private Integer localPort;
    /**
     * The port number to be used in the dynamic http inbound endpoint that will
     * receive the callback
     */
    private Integer remotePort;
    /**
     * The domain to be used in the dynamic http inbound endpoint that will receive
     * the callback
     */
    private String domain;
    /**
     * The dynamically generated url to pass on to the cloud connector. When this url
     * is called the callback flow will be executed
     */
    private String url;
    private String localUrl;
    /**
     * Mule Context
     */
    private MuleContext muleContext;
    /**
     * The flow to be called upon the http callback
     */
    private Flow callbackFlow;
    /**
     * The dynamically created flow
     */
    private Flow flow;
    /**
     * The message processor to be called upon the http callback
     */
    private MessageProcessor callbackMessageProcessor;
    /**
     * Optional path to set up the endpoint
     */
    private String callbackPath;
    /**
     * Whether the the message processor that invokes the callback flow is
     * asynchronous
     */
    private Boolean async;

    private Object connector;

    /**
     * HTTP connector delegate. Allows to handle different connector implementations
     * transparently
     */
    private ConnectorDelegate connectorDelegate;

    private boolean forceOldHttpTransport = false;

    /**
     * Exception Handler
     */
    private MessagingExceptionHandler exceptionHandler;

    public DefaultHttpCallback(Flow callbackFlow,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               Boolean async)
    {
        this.callbackFlow = callbackFlow;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.domain = callbackDomain;
        this.async = async;
    }

    public DefaultHttpCallback(MessageProcessor callbackMessageProcessor,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               Boolean async)
    {
        this.callbackMessageProcessor = callbackMessageProcessor;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.domain = callbackDomain;
        this.async = async;
    }

    public DefaultHttpCallback(MessageProcessor callbackMessageProcessor,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async)
    {
        this.callbackMessageProcessor = callbackMessageProcessor;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
    }

    public DefaultHttpCallback(Flow callbackFlow,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               Boolean async,
                               Object connector)
    {
        this.callbackFlow = callbackFlow;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.domain = callbackDomain;
        this.async = async;
        this.connector = connector;
    }

    public DefaultHttpCallback(MessageProcessor callbackMessageProcessor,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               Boolean async,
                               Object connector)
    {
        this.callbackMessageProcessor = callbackMessageProcessor;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.domain = callbackDomain;
        this.async = async;
        this.connector = connector;
    }

    public DefaultHttpCallback(MessageProcessor callbackMessageProcessor,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async,
                               Object connector)
    {
        this.callbackMessageProcessor = callbackMessageProcessor;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
        this.connector = connector;
    }

    public DefaultHttpCallback(List<MessageProcessor> callbackMessageProcessors,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               Boolean async,
                               Object connector) throws MuleException
    {
        this.callbackMessageProcessor = buildChain(callbackMessageProcessors);
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.domain = callbackDomain;
        this.async = async;
        this.connector = connector;
    }

    public DefaultHttpCallback(List<MessageProcessor> callbackMessageProcessors,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async,
                               Object connector) throws MuleException
    {
        this.callbackMessageProcessor = buildChain(callbackMessageProcessors);
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
        this.connector = connector;
    }

    public DefaultHttpCallback(List<MessageProcessor> callbackMessageProcessors,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               Boolean async) throws MuleException
    {
        this.callbackMessageProcessor = buildChain(callbackMessageProcessors);
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.domain = callbackDomain;
        this.async = async;
    }

    public DefaultHttpCallback(List<MessageProcessor> callbackMessageProcessors,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async) throws MuleException
    {
        this.callbackMessageProcessor = buildChain(callbackMessageProcessors);
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
    }

    public DefaultHttpCallback(List<MessageProcessor> callbackMessageProcessors,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async,
                               MessagingExceptionHandler exceptionHandler) throws MuleException
    {
        this.callbackMessageProcessor = buildChain(callbackMessageProcessors);
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
        this.connectorDelegate = null;
        this.exceptionHandler = exceptionHandler;
    }

    public DefaultHttpCallback(List<MessageProcessor> callbackMessageProcessors,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async,
                               MessagingExceptionHandler exceptionHandler,
                               Object connector) throws MuleException
    {
        this.callbackMessageProcessor = buildChain(callbackMessageProcessors);
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
        this.connectorDelegate = null;
        this.exceptionHandler = exceptionHandler;
        this.connector = connector;
    }

    public DefaultHttpCallback(MessageProcessor callbackMessageProcessor,
                               MuleContext muleContext,
                               String callbackDomain,
                               Integer localPort,
                               Integer remotePort,
                               String callbackPath,
                               Boolean async,
                               MessagingExceptionHandler exceptionHandler,
                               Object connector) throws MuleException
    {
        this.callbackMessageProcessor = callbackMessageProcessor;
        this.muleContext = muleContext;
        this.localPort = localPort;
        this.domain = callbackDomain;
        this.remotePort = remotePort;
        this.callbackPath = callbackPath;
        this.async = async;
        this.connectorDelegate = null;
        this.exceptionHandler = exceptionHandler;
        this.connector = connector;
    }

    /**
     * Retrieves url
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * Sets muleContext
     *
     * @param value Value to set
     */
    public void setMuleContext(MuleContext value)
    {
        this.muleContext = value;
    }

    private ConnectorDelegate getConnectorDelegate(Object connector) throws MuleException
    {
        if (connector != null)
        {
            if (connector instanceof Connector)
            {
                return new OldTransportDelegate((Connector) connector);
            }
            else if (connector instanceof HttpListenerConfig)
            {
                return new HttpConnectorDelegate((HttpListenerConfig) connector);
            }
            else
            {
                throw new IllegalArgumentException(String.format("Could not create OAuth callback at url %s because a connector of class %s was supplied. Valid types are '%s' and '%s' ",
                                                                 url, connector.getClass().getName(), HttpListenerConfig.class.getName(), Connector.class));
            }
        }
        else
        {
            if (forceOldHttpTransport)
            {
                return new OldTransportDelegate(lookupDefaultHttpTransport());
            }
            else
            {
                return new HttpConnectorDelegate();
            }
        }
    }

    private Connector lookupDefaultHttpTransport() throws MuleException
    {
        Connector httpConnector = muleContext.getRegistry().lookupConnector("connector.http.mule.default");
        if (httpConnector != null)
        {
            return httpConnector;
        }
        else
        {
            throw new DefaultMuleException("Could not find connector with name 'connector.http.mule.default'");
        }
    }

    private String buildUrl()
    {
        StringBuilder urlBuilder = new StringBuilder();
        if (!domain.contains("://"))
        {
            if (connectorDelegate != null)
            {
                urlBuilder.append((connectorDelegate.getScheme() + "://"));
            }
            else
            {
                urlBuilder.append("http://");
            }
        }
        urlBuilder.append(domain);
        if ((remotePort != null) && (remotePort != 80))
        {
            urlBuilder.append(":");
            urlBuilder.append(remotePort);
        }
        urlBuilder.append("/");
        if (callbackPath != null)
        {
            urlBuilder.append(callbackPath);
        }
        else
        {
            urlBuilder.append(UUID.randomUUID());
        }
        return urlBuilder.toString();
    }

    public void setForceOldHttpTransport(boolean forceOldHttpTransport)
    {
        this.forceOldHttpTransport = forceOldHttpTransport;
    }

    private MessageProcessor wrapMessageProcessorInAsyncChain(MessageProcessor messageProcessor)
            throws MuleException
    {
        AsyncMessageProcessorsFactoryBean asyncMessageProcessorsFactoryBean = new AsyncMessageProcessorsFactoryBean();
        asyncMessageProcessorsFactoryBean.setMuleContext(muleContext);
        asyncMessageProcessorsFactoryBean.setMessageProcessors(Arrays.asList(messageProcessor));
        asyncMessageProcessorsFactoryBean.setProcessingStrategy(new AsynchronousProcessingStrategy());
        try
        {
            return ((MessageProcessor) asyncMessageProcessorsFactoryBean.getObject());
        }
        catch (Exception e)
        {
            throw new FlowConstructInvalidException(e);
        }
    }

    private InboundEndpoint createHttpInboundEndpoint() throws MuleException
    {
        EndpointURIEndpointBuilder inBuilder = new EndpointURIEndpointBuilder(localUrl, muleContext);
        inBuilder.setConnector((Connector) connectorDelegate.getConnector());
        inBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        EndpointFactory endpointFactory = muleContext.getEndpointFactory();
        return endpointFactory.getInboundEndpoint(inBuilder);
    }

    /**
     * Starts a http inbound endpoint served by the underlying connector. It also
     * builds a processor chain to be invoked upon callback reception.
     */
    public void start() throws MuleException
    {
        String dynamicFlowName = String.format("DynamicFlow-%s:%d/%s", domain, localPort, callbackPath);
        flow = new Flow(dynamicFlowName, muleContext);

        connectorDelegate = getConnectorDelegate(connector);
        url = buildUrl();
        this.localUrl = url.replaceFirst(domain, "localhost");
        if (localUrl.indexOf((":" + String.valueOf(remotePort))) == -1)
        {
            this.localUrl = localUrl.replaceFirst("localhost", ("localhost:" + String.valueOf(localPort)));
        }
        else
        {
            this.localUrl = localUrl.replaceFirst(String.valueOf(remotePort), String.valueOf(localPort));
        }

        MessageSource source = connectorDelegate.getMessageSource();

        if (source instanceof FlowConstructAware)
        {
            ((FlowConstructAware) source).setFlowConstruct(flow);
        }

        flow.setMessageSource(source);
        MessageProcessor messageProcessor;
        if (callbackFlow != null)
        {
            messageProcessor = new DefaultHttpCallback.FlowRefMessageProcessor();
        }
        else
        {
            messageProcessor = callbackMessageProcessor;
        }
        if (async)
        {
            messageProcessor = wrapMessageProcessorInAsyncChain(messageProcessor);
        }
        List<MessageProcessor> messageProcessors = new ArrayList<>();
        messageProcessors.add(messageProcessor);
        flow.setMessageProcessors(messageProcessors);
        if (exceptionHandler != null)
        {
            flow.setExceptionListener(exceptionHandler);
        }
        muleContext.getRegistry().registerObject(dynamicFlowName, flow);
        LOGGER.debug(String.format("Created flow with http inbound endpoint listening at: %s", url));
    }

    /**
     * Stops and disposes the processor chain and the inbound endpoint.
     */
    public void stop() throws MuleException
    {
        if (flow != null)
        {
            String flowName = flow.getName();
            flow.stop();
            flow.dispose();
            muleContext.getRegistry().unregisterObject(flowName);
            LOGGER.debug("Http callback flow stopped");
        }
    }

    private static MessageProcessor buildChain(List<MessageProcessor> messageProcessors) throws MuleException
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        for (Object messageProcessor : messageProcessors)
        {
            if (messageProcessor instanceof MessageProcessor)
            {
                builder.chain(((MessageProcessor) messageProcessor));
                continue;
            }
            if (messageProcessor instanceof MessageProcessorBuilder)
            {
                builder.chain(((MessageProcessorBuilder) messageProcessor));
                continue;
            }
        }
        return builder.build();
    }

    public Flow getFlow()
    {
        return flow;
    }

    public class FlowRefMessageProcessor implements MessageProcessor
    {

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return callbackFlow.process(event);
        }

    }

    private interface ConnectorDelegate<T>
    {

        String getScheme();

        MessageSource getMessageSource() throws MuleException;

        T getConnector();

    }

    private class OldTransportDelegate implements ConnectorDelegate<Connector>
    {

        private final Connector connector;

        private OldTransportDelegate(Connector connector)
        {
            this.connector = connector;
        }

        @Override
        public String getScheme()
        {
            return connector.getProtocol();
        }

        @Override
        public MessageSource getMessageSource() throws MuleException
        {
            return createHttpInboundEndpoint();
        }

        @Override
        public Connector getConnector()
        {
            return connector;
        }
    }

    private class HttpConnectorDelegate implements ConnectorDelegate<HttpListenerConfig>
    {

        private final HttpListenerConfig listenerConfig;
        private final HttpListener messageSource;

        private HttpConnectorDelegate(HttpListenerConfig listenerConfig) throws MuleException
        {
            this.listenerConfig = listenerConfig;
            messageSource = new HttpListenerBuilder(muleContext)
                    .setListenerConfig(listenerConfig)
                    .setPath(sanitizeCallbackPath())
                    .setFlow(flow)
                    .build();
        }

        public HttpConnectorDelegate() throws MuleException
        {
            messageSource = new HttpListenerBuilder(muleContext)
                    .setPath(sanitizeCallbackPath())
                    .setFlow(flow)
                    .setHost(domain)
                    .setPort(localPort)
                    .build();
            listenerConfig = messageSource.getConfig();
        }

        private String sanitizeCallbackPath()
        {
            return StringUtils.isBlank(callbackPath) ? "/" : callbackPath;
        }

        @Override
        public String getScheme()
        {
            return listenerConfig.hasTlsConfig() ? HTTPS.getScheme() : HTTP.getScheme();
        }

        @Override
        public MessageSource getMessageSource() throws MuleException
        {
            return this.messageSource;
        }

        @Override
        public HttpListenerConfig getConnector()
        {
            return listenerConfig;
        }
    }

}
