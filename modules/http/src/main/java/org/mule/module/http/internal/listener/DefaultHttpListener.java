/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static java.lang.String.format;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;

import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.execution.MessageProcessingManager;
import org.mule.module.http.api.HttpConstants.HttpStatus;
import org.mule.module.http.api.listener.HttpListener;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.module.http.internal.listener.async.RequestHandler;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.module.http.internal.listener.matcher.AcceptsAllMethodsRequestMatcher;
import org.mule.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.module.http.internal.listener.matcher.MethodRequestMatcher;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpListener implements HttpListener, Initialisable, MuleContextAware, FlowConstructAware
{

    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpListener.class);

    public static final String SERVER_PROBLEM = "Server encountered a problem";

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
    private String[] parsedAllowedMethods;
    private ListenerPath listenerPath;

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

    @Override
    public HttpListenerConfig getConfig()
    {
        return config;
    }

    @Override
    public synchronized void start() throws MuleException
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
                try
                {
                    final HttpMessageProcessorTemplate httpMessageProcessorTemplate = new HttpMessageProcessorTemplate(createEvent(requestContext), messageProcessor, responseCallback, responseBuilder, errorResponseBuilder);
                    final HttpMessageProcessContext messageProcessContext = new HttpMessageProcessContext(DefaultHttpListener.this, flowConstruct, config.getWorkManager(), muleContext.getExecutionClassLoader());
                    messageProcessingManager.processMessage(httpMessageProcessorTemplate, messageProcessContext);
                }
                catch (HttpRequestParsingException | IllegalArgumentException e)
                {
                    logger.warn("Exception occurred parsing request:", e);
                    sendErrorResponse(BAD_REQUEST, getEscapedErrorBody(e), responseCallback);
                }
                catch (RuntimeException e)
                {
                    logger.warn("Exception occurred processing request:", e);
                    sendErrorResponse(INTERNAL_SERVER_ERROR, SERVER_PROBLEM, responseCallback);
                }
                finally
                {
                    RequestContext.clear();
                }
            }

            private void sendErrorResponse(final HttpStatus status, String message, HttpResponseReadyCallback responseCallback)
            {

                byte[] responseData = message.getBytes();
                HttpResponse response = new org.mule.module.http.internal.domain.response.HttpResponseBuilder()
                    .setStatusCode(status.getStatusCode())
                    .setReasonPhrase(status.getReasonPhrase())
                    .setEntity(new ByteArrayHttpEntity(responseData))
                    .addHeader(CONTENT_LENGTH, Integer.toString(responseData.length))
                    .build();

                responseCallback.responseReady(response, new ResponseStatusCallback()
                {
                    @Override
                    public void responseSendFailure(Throwable exception)
                    {
                        logger.warn("Error while sending {} response {}", status.getStatusCode(), exception.getMessage());
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Exception thrown", exception);
                        }
                    }

                    @Override
                    public void responseSendSuccessfully()
                    {
                    }
                });
            }

            private String getEscapedErrorBody(Exception e) {
                return format("HTTP request parsing failed with error: \"%s\"", escapeHtml(e.getMessage()));
            }
        };
    }

    private MuleEvent createEvent(HttpRequestContext requestContext) throws HttpRequestParsingException
    {
        MuleEvent muleEvent = HttpRequestToMuleEvent.transform(requestContext, muleContext, flowConstruct, parseRequest, listenerPath);
        // Update RequestContext ThreadLocal for backwards compatibility
        OptimizedRequestContext.unsafeSetEvent(muleEvent);
        return muleEvent;
    }

    @Override
    public synchronized void initialise() throws InitialisationException
    {
        if (allowedMethods != null)
        {
            parsedAllowedMethods = extractAllowedMethods();
            methodRequestMatcher = new MethodRequestMatcher(parsedAllowedMethods);
        }
        if (responseBuilder == null)
        {
            responseBuilder = HttpResponseBuilder.emptyInstance(muleContext);
        }

        LifecycleUtils.initialiseIfNeeded(responseBuilder);

        if (errorResponseBuilder == null)
        {
            errorResponseBuilder = HttpResponseBuilder.emptyInstance(muleContext);
        }

        LifecycleUtils.initialiseIfNeeded(errorResponseBuilder);

        path = HttpParser.sanitizePathWithStartSlash(path);
        listenerPath = config.getFullListenerPath(path);
        path = listenerPath.getResolvedPath();
        responseBuilder.setResponseStreaming(responseStreamingMode);
        validatePath();
        parseRequest = config.resolveParseRequest(parseRequest);
        try
        {
            messageProcessingManager = DefaultHttpListener.this.muleContext.getRegistry().lookupObject(MessageProcessingManager.class);
            requestHandlerManager = this.config.addRequestHandler(new ListenerRequestMatcher(methodRequestMatcher, path), getRequestHandler());
        }
        catch (Exception e)
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
                    throw new InitialisationException(CoreMessages.createStaticMessage(
                      format("Http Listener with path %s contains duplicated uri param names", this.path)), this);
                }
                uriParamNames.add(uriParamName);
            }
            else
            {
                if (pathPart.contains("*") && pathPart.length() > 1)
                {
                    throw new InitialisationException(CoreMessages.createStaticMessage(
                      format("Http Listener with path %s contains an invalid use of a wildcard. Wildcards can only be used at the end of the path (i.e.: /path/*) or between / characters (.i.e.: /path/*/anotherPath))", this.path)), this);
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
    public synchronized void stop() throws MuleException
    {
        if (muleContext.isStopping())
        {
            config.stop();
        }
        requestHandlerManager.stop();
    }

    @Override
    public void dispose()
    {
        requestHandlerManager.dispose();
    }

    @Override
    public String getPath()
    {
        return path;
    }

    @Override
    public String[] getAllowedMethods()
    {
        return parsedAllowedMethods;
    }
}
