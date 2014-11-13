/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.execution.MessageProcessingManager;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.HttpStreamingType;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.matcher.AcceptsAllMethodsRequestMatcher;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.module.http.internal.listener.matcher.MethodRequestMatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultHttpListener implements HttpListener, Initialisable, MuleContextAware, FlowConstructAware, Startable, Stoppable, Disposable
{

    private String path;
    private String allowedMethods;
    private Boolean parseRequest;
    private MessageProcessor messageProcessor;
    private MethodRequestMatcher methodRequestMatcher = AcceptsAllMethodsRequestMatcher.instance();
    private MuleContext muleContext;
    private FlowConstruct flowConstruct;
    private DefaultHttpListenerConfig config;
    private HttpResponseBuilder responseBuilder;
    private HttpResponseBuilder errorResponseBuilder;
    private HttpStreamingType responseStreamingMode = HttpStreamingType.AUTO;
    private RequestHandlerManager requestHandlerManager;
    private MessageProcessingManager messageProcessingManager;

    @Override
    public void setListener(final MessageProcessor messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    public void setPath(final String path)
    {
        this.path = path;
    }

    public void setAllowedMethods(final String allowedMethods)
    {
        this.allowedMethods = allowedMethods;
    }

    public void setConfig(DefaultHttpListenerConfig config)
    {
        this.config = config;
    }

    public void setResponseBuilder(HttpResponseBuilder responseBuilder)
    {
        this.responseBuilder = responseBuilder;
    }

    public void setErrorResponseBuilder(HttpResponseBuilder errorResponseBuilder)
    {
        this.errorResponseBuilder = errorResponseBuilder;
    }

    public void setResponseStreamingMode(HttpStreamingType responseStreamingMode)
    {
        this.responseStreamingMode = responseStreamingMode;
    }

    public void setParseRequest(boolean parseRequest)
    {
        this.parseRequest = parseRequest;
    }

    public HttpListenerConfig getConfig()
    {
        return config;
    }

    @Override
    public void start() throws MuleException
    {
        requestHandlerManager.start();
    }

    private RequestHandler getRequestHandler()
    {
        return new RequestHandler()
        {
            @Override
            public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback)
            {
                final HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(createEvent(requestContext, path), messageProcessor, responseCallback, responseBuilder, errorResponseBuilder);
                final HttpMessageProcessContext messageProcessContext = new HttpMessageProcessContext(DefaultHttpListener.this, flowConstruct, config.getWorkManager(), muleContext.getExecutionClassLoader());
                messageProcessingManager.processMessage(httpMessageProcessorTemplate, messageProcessContext);
            }
        };
    }

    private MuleEvent createEvent(HttpRequestContext requestContext, String listenerPath)
    {
        return HttpRequestToMuleEvent.transform(requestContext, muleContext, flowConstruct, config.resolveParseRequest(parseRequest), listenerPath);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (config == null)
        {
            config = DefaultHttpListenerConfig.emptyConfig(muleContext);
        }
        if (allowedMethods != null)
        {
            methodRequestMatcher = new MethodRequestMatcher(extractAllowedMethods());
        }
        if (responseBuilder == null)
        {
            responseBuilder = HttpResponseBuilder.emptyInstance(muleContext);
        }
        if (errorResponseBuilder == null)
        {
            errorResponseBuilder = HttpResponseBuilder.emptyInstance(muleContext);
        }
        path = HttpParser.sanitizePathWithStartSlash(path);
        path = config.resolvePath(path);
        responseBuilder.setResponseStreaming(responseStreamingMode);
        validatePath();
        messageProcessingManager = DefaultHttpListener.this.muleContext.getRegistry().get(MuleProperties.OBJECT_DEFAULT_MESSAGE_PROCESSING_MANAGER);
        try
        {
            requestHandlerManager = this.config.addRequestHandler(new ListenerRequestMatcher(methodRequestMatcher, path), getRequestHandler());
        }
        catch (IOException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    private void validatePath() throws InitialisationException
    {
        final String[] pathParts = this.path.split("/");
        List<String> uriParamNames = new ArrayList<>();
        for (String pathPart : pathParts)
        {
            if (pathPart.startsWith("{") && pathPart.endsWith("}"))
            {
                String uriParamName = pathPart.substring(1, pathPart.length() - 1);
                if (uriParamNames.contains(uriParamName))
                {
                    throw new InitialisationException(CoreMessages.createStaticMessage(String.format("Http Listener with path %s contains duplicated uri param names", this.path)), this);
                }
                uriParamNames.add(uriParamName);
            }
            else
            {
                if (pathPart.contains("*") && pathPart.length() > 1)
                {
                    throw new InitialisationException(CoreMessages.createStaticMessage(String.format("Http Listener with path %s contains an invalid use of a wildcard. Wildcards can only be used at the end of the path (i.e.: /path/*) or between / characters (.i.e.: /path/*/anotherPath))", this.path)), this);
                }
            }
        }
    }

    private String[] extractAllowedMethods() throws InitialisationException
    {
        final String[] values = this.allowedMethods.split(",");
        final String[] normalizedValues = new String[values.length];
        int normalizedValueIndex = 0;
        for (String value : values)
        {
            normalizedValues[normalizedValueIndex] = value.trim().toUpperCase();
            normalizedValueIndex++;
        }
        return normalizedValues;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void stop() throws MuleException
    {
        requestHandlerManager.stop();
    }

    @Override
    public void dispose()
    {
        requestHandlerManager.dispose();
    }
}
