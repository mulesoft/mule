/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.module.http.api.HttpConstants.ALL_INTERFACES_IP;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.HOST;
import static org.mule.runtime.module.http.internal.domain.HttpProtocol.HTTP_0_9;
import static org.mule.runtime.module.http.internal.domain.HttpProtocol.HTTP_1_0;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.MessageProcessingManager;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.SystemUtils;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.HttpConstants.HttpStatus;
import org.mule.runtime.module.http.api.listener.HttpListener;
import org.mule.runtime.module.http.api.listener.HttpListenerConfig;
import org.mule.runtime.module.http.api.requester.HttpStreamingType;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;
import org.mule.runtime.module.http.internal.listener.async.HttpResponseReadyCallback;
import org.mule.runtime.module.http.internal.listener.async.RequestHandler;
import org.mule.runtime.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.runtime.module.http.internal.listener.matcher.AcceptsAllMethodsRequestMatcher;
import org.mule.runtime.module.http.internal.listener.matcher.ListenerRequestMatcher;
import org.mule.runtime.module.http.internal.listener.matcher.MethodRequestMatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultHttpListener implements HttpListener, Initialisable, MuleContextAware, FlowConstructAware {

  private static final Logger logger = LoggerFactory.getLogger(DefaultHttpListener.class);

  public static final String SERVER_PROBLEM = "Server encountered a problem";

  private String path;
  private String allowedMethods;
  private Boolean parseRequest;
  private Processor messageProcessor;
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
  public void setListener(final Processor messageProcessor) {
    this.messageProcessor = messageProcessor;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  public void setAllowedMethods(final String allowedMethods) {
    this.allowedMethods = allowedMethods;
  }

  public void setConfig(DefaultHttpListenerConfig config) {
    this.config = config;
  }

  public void setResponseBuilder(HttpResponseBuilder responseBuilder) {
    this.responseBuilder = responseBuilder;
  }

  public void setErrorResponseBuilder(HttpResponseBuilder errorResponseBuilder) {
    this.errorResponseBuilder = errorResponseBuilder;
  }

  public void setResponseStreamingMode(HttpStreamingType responseStreamingMode) {
    this.responseStreamingMode = responseStreamingMode;
  }

  public void setParseRequest(boolean parseRequest) {
    this.parseRequest = parseRequest;
  }

  @Override
  public HttpListenerConfig getConfig() {
    return config;
  }

  @Override
  public synchronized void start() throws MuleException {
    requestHandlerManager.start();
  }

  private RequestHandler getRequestHandler() {
    return new RequestHandler() {

      @Override
      public void handleRequest(HttpRequestContext requestContext, HttpResponseReadyCallback responseCallback) {
        try {
          final HttpMessageProcessorTemplate httpMessageProcessorTemplate =
              new HttpMessageProcessorTemplate(createEvent(requestContext), messageProcessor, responseCallback, responseBuilder,
                                               errorResponseBuilder);
          final HttpMessageProcessContext messageProcessContext =
              new HttpMessageProcessContext(DefaultHttpListener.this, flowConstruct, config.getWorkManager(),
                                            muleContext.getExecutionClassLoader());
          messageProcessingManager.processMessage(httpMessageProcessorTemplate, messageProcessContext);
        } catch (HttpRequestParsingException | IllegalArgumentException e) {
          logger.warn("Exception occurred parsing request:", e);
          sendErrorResponse(BAD_REQUEST, e.getMessage(), responseCallback);
        } catch (RuntimeException e) {
          logger.warn("Exception occurred processing request:", e);
          sendErrorResponse(INTERNAL_SERVER_ERROR, SERVER_PROBLEM, responseCallback);
        } finally {
          setCurrentEvent(null);
        }
      }

      private void sendErrorResponse(final HttpStatus status, String message, HttpResponseReadyCallback responseCallback) {
        responseCallback.responseReady(new org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder()
            .setStatusCode(status.getStatusCode())
            .setReasonPhrase(status.getReasonPhrase())
            .setEntity(new ByteArrayHttpEntity(message.getBytes()))
            .build(), new ResponseStatusCallback() {

              @Override
              public void responseSendFailure(Throwable exception) {
                logger.warn("Error while sending {} response {}", status.getStatusCode(), exception.getMessage());
                if (logger.isDebugEnabled()) {
                  logger.debug("Exception thrown", exception);
                }
              }

              @Override
              public void responseSendSuccessfully() {}
            });
      }
    };
  }

  private Event createEvent(HttpRequestContext requestContext) throws HttpRequestParsingException {
    Event muleEvent = Event.builder(create(flowConstruct, resolveUri(requestContext).toString())).message(HttpRequestToMuleEvent
        .transform(requestContext, SystemUtils.getDefaultEncoding(muleContext), parseRequest, listenerPath))
        .exchangePattern(REQUEST_RESPONSE).flow(flowConstruct).session(new DefaultMuleSession()).build();
    // Update RequestContext ThreadLocal for backwards compatibility
    setCurrentEvent(muleEvent);
    return muleEvent;
  }

  private static URI resolveUri(final HttpRequestContext requestContext) {
    try {
      String hostAndPort = resolveTargetHost(requestContext.getRequest());
      String[] hostAndPortParts = hostAndPort.split(":");
      String host = hostAndPortParts[0];
      int port = requestContext.getScheme().equals(HttpConstants.Protocols.HTTP) ? 80 : 4343;
      if (hostAndPortParts.length > 1) {
        port = Integer.valueOf(hostAndPortParts[1]);
      }
      return new URI(requestContext.getScheme(), null, host, port, requestContext.getRequest().getPath(), null, null);
    } catch (URISyntaxException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * See <a href="http://www8.org/w8-papers/5c-protocols/key/key.html#SECTION00070000000000000000" >Internet address
   * conservation</a>.
   */
  private static String resolveTargetHost(HttpRequest request) {
    String hostHeaderValue = request.getHeaderValueIgnoreCase(HOST);
    if (HTTP_1_0.equals(request.getProtocol()) || HTTP_0_9.equals(request.getProtocol())) {
      return hostHeaderValue == null ? ALL_INTERFACES_IP : hostHeaderValue;
    } else {
      if (hostHeaderValue == null) {
        throw new IllegalArgumentException("Missing 'host' header");
      } else {
        return hostHeaderValue;
      }
    }
  }

  @Override
  public synchronized void initialise() throws InitialisationException {
    if (allowedMethods != null) {
      parsedAllowedMethods = extractAllowedMethods();
      methodRequestMatcher = new MethodRequestMatcher(parsedAllowedMethods);
    }
    if (responseBuilder == null) {
      responseBuilder = HttpResponseBuilder.emptyInstance(muleContext);
    }

    LifecycleUtils.initialiseIfNeeded(responseBuilder);

    if (errorResponseBuilder == null) {
      errorResponseBuilder = HttpResponseBuilder.emptyInstance(muleContext);
    }

    LifecycleUtils.initialiseIfNeeded(errorResponseBuilder);

    path = HttpParser.sanitizePathWithStartSlash(path);
    listenerPath = config.getFullListenerPath(path);
    path = listenerPath.getResolvedPath();
    responseBuilder.setResponseStreaming(responseStreamingMode);
    validatePath();
    parseRequest = config.resolveParseRequest(parseRequest);
    try {
      messageProcessingManager = DefaultHttpListener.this.muleContext.getRegistry().lookupObject(MessageProcessingManager.class);
      requestHandlerManager =
          this.config.addRequestHandler(new ListenerRequestMatcher(methodRequestMatcher, path), getRequestHandler());
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  private void validatePath() throws InitialisationException {
    final String[] pathParts = this.path.split("/");
    List<String> uriParamNames = new ArrayList<>();
    for (String pathPart : pathParts) {
      if (pathPart.startsWith("{") && pathPart.endsWith("}")) {
        String uriParamName = pathPart.substring(1, pathPart.length() - 1);
        if (uriParamNames.contains(uriParamName)) {
          throw new InitialisationException(CoreMessages
              .createStaticMessage(String.format("Http Listener with path %s contains duplicated uri param names", this.path)),
                                            this);
        }
        uriParamNames.add(uriParamName);
      } else {
        if (pathPart.contains("*") && pathPart.length() > 1) {
          throw new InitialisationException(CoreMessages.createStaticMessage(String.format(
                                                                                           "Http Listener with path %s contains an invalid use of a wildcard. Wildcards can only be used at the end of the path (i.e.: /path/*) or between / characters (.i.e.: /path/*/anotherPath))",
                                                                                           this.path)),
                                            this);
        }
      }
    }
  }

  private String[] extractAllowedMethods() throws InitialisationException {
    final String[] values = this.allowedMethods.split(",");
    final String[] normalizedValues = new String[values.length];
    int normalizedValueIndex = 0;
    for (String value : values) {
      normalizedValues[normalizedValueIndex] = value.trim().toUpperCase();
      normalizedValueIndex++;
    }
    return normalizedValues;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  @Override
  public synchronized void stop() throws MuleException {
    requestHandlerManager.stop();
  }

  @Override
  public void dispose() {
    requestHandlerManager.dispose();
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String[] getAllowedMethods() {
    return parsedAllowedMethods;
  }
}
