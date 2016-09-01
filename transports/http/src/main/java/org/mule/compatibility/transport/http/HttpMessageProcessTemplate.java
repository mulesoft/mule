/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.apache.commons.httpclient.HttpVersion.HTTP_1_1;
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_CONTEXT_PATH_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_CONTEXT_URI_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_RELATIVE_PATH_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_REQUEST_PATH_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_REQUEST_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConnector.HTTP_STATUS_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.CRLF;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_CONNECTION;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_EXPECT;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_EXPECT_CONTINUE_REQUEST_VALUE;
import static org.mule.compatibility.transport.http.HttpConstants.HEADER_X_FORWARDED_FOR;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_CONNECT;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_DELETE;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_GET;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_HEAD;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_OPTIONS;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_PATCH;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_POST;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_PUT;
import static org.mule.compatibility.transport.http.HttpConstants.METHOD_TRACE;
import static org.mule.compatibility.transport.http.HttpConstants.SC_BAD_REQUEST;
import static org.mule.compatibility.transport.http.HttpConstants.SC_CONTINUE;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_PROXY_ADDRESS;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_CLIENT_ADDRESS;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.message.MuleCompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.compatibility.core.transport.AbstractTransportMessageProcessTemplate;
import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.execution.EndPhaseTemplate;
import org.mule.runtime.core.execution.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseDispatchException;
import org.mule.runtime.core.execution.ThrottlingPhaseTemplate;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.http.internal.listener.HttpMessageProcessorTemplate;
import org.mule.runtime.module.http.internal.listener.HttpThrottlingHeadersMapBuilder;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;

public class HttpMessageProcessTemplate extends AbstractTransportMessageProcessTemplate<HttpMessageReceiver, HttpConnector>
    implements RequestResponseFlowProcessingPhaseTemplate, ThrottlingPhaseTemplate, EndPhaseTemplate {

  public static final int MESSAGE_DISCARD_STATUS_CODE = HttpMessageProcessorTemplate.MESSAGE_DISCARD_STATUS_CODE;
  public static final String MESSAGE_THROTTLED_REASON_PHRASE = "API calls exceeded";

  public static final String X_RATE_LIMIT_LIMIT_HEADER = HttpMessageProcessorTemplate.X_RATE_LIMIT_LIMIT_HEADER;
  public static final String X_RATE_LIMIT_REMAINING_HEADER = HttpMessageProcessorTemplate.X_RATE_LIMIT_REMAINING_HEADER;
  public static final String X_RATE_LIMIT_RESET_HEADER = HttpMessageProcessorTemplate.X_RATE_LIMIT_RESET_HEADER;

  private final HttpServerConnection httpServerConnection;
  private HttpRequest request;
  private boolean badRequest;
  private Latch messageProcessedLatch = new Latch();
  private RequestLine requestLine;
  private boolean failureResponseSentToClient;
  private HttpThrottlingHeadersMapBuilder httpThrottlingHeadersMapBuilder;

  public HttpMessageProcessTemplate(final HttpMessageReceiver messageReceiver, final HttpServerConnection httpServerConnection) {
    super(messageReceiver);
    this.httpServerConnection = httpServerConnection;
    this.httpThrottlingHeadersMapBuilder = new HttpThrottlingHeadersMapBuilder();
  }

  @Override
  public void sendResponseToClient(MuleEvent responseMuleEvent) throws MuleException {
    try {
      if (logger.isTraceEnabled()) {
        logger.trace("Sending http response");
      }
      MuleMessage returnMessage = responseMuleEvent == null ? null : responseMuleEvent.getMessage();

      Object tempResponse;
      if (returnMessage != null) {
        tempResponse = returnMessage.getPayload();
      } else {
        tempResponse = null;
      }
      // This removes the need for users to explicitly adding the response transformer
      // ObjectToHttpResponse in their config
      HttpResponse response;
      if (tempResponse instanceof HttpResponse) {
        response = (HttpResponse) tempResponse;
      } else {
        response = transformResponse(tempResponse);
      }

      response.setupKeepAliveFromRequestVersion(request.getRequestLine().getHttpVersion());
      HttpConnector httpConnector = (HttpConnector) getMessageReceiver().getEndpoint().getConnector();
      response.disableKeepAlive(!httpConnector.isKeepAlive());

      Header connectionHeader = request.getFirstHeader("Connection");
      boolean endpointOverride = getMessageReceiver().getEndpoint().getProperty("keepAlive") != null;
      boolean endpointKeepAliveValue = getEndpointKeepAliveValue(getMessageReceiver().getEndpoint());

      if (endpointOverride) {
        response.disableKeepAlive(!endpointKeepAliveValue);
      } else {
        response.disableKeepAlive(!httpConnector.isKeepAlive());
      }

      if (connectionHeader != null) {
        String value = connectionHeader.getValue();
        if ("keep-alive".equalsIgnoreCase(value) && endpointKeepAliveValue) {
          response.setKeepAlive(true);

          if (response.getHttpVersion().equals(HttpVersion.HTTP_1_0)) {
            connectionHeader = new Header(HEADER_CONNECTION, "Keep-Alive");
            response.setHeader(connectionHeader);
          }
        } else if ("close".equalsIgnoreCase(value) || !endpointKeepAliveValue) {
          response.setKeepAlive(false);
        }
      } else if (request.getRequestLine().getHttpVersion().equals(HTTP_1_1)) {
        response.setKeepAlive(endpointKeepAliveValue);
      }

      try {
        httpServerConnection.writeResponse(response, getThrottlingHeaders());
      } catch (Exception e) {
        throw new ResponseDispatchException(responseMuleEvent, e);
      }
      if (logger.isTraceEnabled()) {
        logger.trace("HTTP response sent successfully");
      }
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Exception while sending http response", e);
      }
      throw new MessagingException(responseMuleEvent, e);
    }
  }

  @Override
  public void sendFailureResponseToClient(MessagingException messagingException) throws MuleException {
    MuleEvent response = messagingException.getEvent();
    MessagingException e = getExceptionForCreatingFailureResponse(messagingException, response);
    String temp = ExceptionHelper.getErrorMapping(getInboundEndpoint().getConnector().getProtocol(),
                                                  messagingException.getClass(), getMuleContext());
    int httpStatus = Integer.valueOf(temp);
    try {
      sendFailureResponseToClient(e, httpStatus);
    } catch (IOException ioException) {
      throw new DefaultMuleException(ioException);
    }
    failureResponseSentToClient = true;
  }

  private MessagingException getExceptionForCreatingFailureResponse(MessagingException messagingException, MuleEvent response) {
    MessagingException e = messagingException;
    if (response != null &&
        response.getError() != null &&
        response.getError().getException() instanceof MessagingException) {
      e = (MessagingException) response.getError().getException();
    }
    return e;
  }

  @Override
  public void afterFailureProcessingFlow(MuleException exception) throws MuleException {
    if (!failureResponseSentToClient) {
      String temp = ExceptionHelper.getErrorMapping(getConnector().getProtocol(), exception.getClass(), getMuleContext());
      int httpStatus = Integer.valueOf(temp);
      try {
        sendFailureResponseToClient(httpStatus, exception.getMessage());
      } catch (Exception e) {
        final String errorMessage = "Exception sending http response after error";
        logger.warn(errorMessage + ": " + e.getMessage());
        if (logger.isDebugEnabled()) {
          logger.debug(errorMessage, e);
        }
      }
    }
  }

  @Override
  public MuleEvent beforeRouteEvent(MuleEvent muleEvent) throws MuleException {
    try {
      sendExpect100(request, muleEvent);
      return muleEvent;
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
  }

  private void sendExpect100(HttpRequest request, MuleEvent muleEvent) throws MuleException, IOException {
    RequestLine requestLine = request.getRequestLine();

    // respond with status code 100, for Expect handshake
    // according to rfc 2616 and http 1.1
    // the processing will continue and the request will be fully
    // read immediately after
    HttpVersion requestVersion = requestLine.getHttpVersion();
    if (HTTP_1_1.equals(requestVersion)) {
      Header expectHeader = request.getFirstHeader(HEADER_EXPECT);
      if (expectHeader != null) {
        String expectHeaderValue = expectHeader.getValue();
        if (HEADER_EXPECT_CONTINUE_REQUEST_VALUE.equals(expectHeaderValue)) {
          HttpResponse expected = new HttpResponse();
          expected.setStatusLine(requestLine.getHttpVersion(), SC_CONTINUE);
          expected.setKeepAlive(true);
          final MuleEvent event = MuleEvent.builder(muleEvent.getContext())
              .message(MuleMessage.builder().payload(expected).build()).flow(getFlowConstruct()).build();
          populateFieldsFromInboundEndpoint(event, getInboundEndpoint());

          setCurrentEvent(event);
          httpServerConnection.writeResponse(expected);
        }
      }
    }
  }

  /**
   * Check if endpoint has a keep-alive property configured. Note the translation from keep-alive in the schema to keepAlive here.
   */
  private boolean getEndpointKeepAliveValue(ImmutableEndpoint ep) {
    String value = (String) ep.getProperty("keepAlive");
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return true;
  }

  protected HttpResponse transformResponse(Object response) throws MuleException {
    MuleMessage message;
    if (response instanceof MuleMessage) {
      message = (MuleMessage) response;
    } else {
      message = MuleMessage.builder().payload(response).build();
    }
    // TODO RM*: Maybe we can have a generic Transformer wrapper rather that using DefaultMuleMessage (or another static utility
    // class
    return (HttpResponse) getMuleContext().getTransformationService().applyTransformers(message, null, getMessageReceiver()
        .getResponseTransportTransformers())
        .getPayload();
  }

  @Override
  protected MuleCompatibilityMessage createMessageFromSource(Object message) throws MuleException {
    MuleMessage muleMessage = super.createMessageFromSource(message);
    MuleCompatibilityMessageBuilder messageBuilder = new MuleCompatibilityMessageBuilder(muleMessage);

    String path = muleMessage.getInboundProperty(HTTP_REQUEST_PROPERTY);
    int i = path.indexOf('?');
    if (i > -1) {
      path = path.substring(0, i);
    }

    messageBuilder.addInboundProperty(HTTP_REQUEST_PATH_PROPERTY, path);

    if (logger.isDebugEnabled()) {
      logger.debug(muleMessage.getInboundProperty(HTTP_REQUEST_PROPERTY));
    }

    // determine if the request path on this request denotes a different receiver
    // final MessageReceiver receiver = getTargetReceiver(message, endpoint);

    // the response only needs to be transformed explicitly if
    // A) the request was not served or B) a null result was returned
    String contextPath = HttpConnector.normalizeUrl(getInboundEndpoint().getEndpointURI().getPath());
    messageBuilder.addInboundProperty(HTTP_CONTEXT_PATH_PROPERTY, contextPath);

    messageBuilder.addInboundProperty(HTTP_CONTEXT_URI_PROPERTY, getInboundEndpoint().getEndpointURI().getAddress());

    messageBuilder.addInboundProperty(HTTP_RELATIVE_PATH_PROPERTY, processRelativePath(contextPath, path));

    processRemoteAddresses(messageBuilder, muleMessage);

    return messageBuilder.build();
  }

  /**
   * For a given MuleMessage will set the <code>MULE_REMOTE_CLIENT_ADDRESS</code> property taking into consideration if the header
   * <code>X-Forwarded-For</code> is present in the request or not. In case it is, this method will also set the
   * <code>MULE_PROXY_ADDRESS</code> property. If a proxy address is not passed in <code>X-Forwarded-For</code>, the connection
   * address will be set as <code>MULE_PROXY_ADDRESS</code>.
   *
   * @param muleMessageBuilder MuleMessageBuilder to be enriched
   * @param original original message
   * @see <a href="https://en.wikipedia.org/wiki/X-Forwarded-For">https://en.wikipedia.org/wiki/X-Forwarded-For</a>
   */
  protected void processRemoteAddresses(MuleMessage.Builder muleMessageBuilder, MuleMessage original) {
    String xForwardedFor = original.getInboundProperty(HEADER_X_FORWARDED_FOR);

    if (StringUtils.isEmpty(xForwardedFor)) {
      muleMessageBuilder.addInboundProperty(MULE_REMOTE_CLIENT_ADDRESS, httpServerConnection.getRemoteClientAddress());
      return;
    }

    String[] xForwardedForItems = StringUtils.splitAndTrim(xForwardedFor, ",");
    if (!ArrayUtils.isEmpty(xForwardedForItems)) {
      muleMessageBuilder.addInboundProperty(MULE_REMOTE_CLIENT_ADDRESS, xForwardedForItems[0]);
      if (xForwardedForItems.length > 1) {
        muleMessageBuilder.addInboundProperty(MULE_PROXY_ADDRESS, xForwardedForItems[xForwardedForItems.length - 1]);
      } else {
        // If only one address has been passed, we can assume the connection address is a proxy
        muleMessageBuilder.addInboundProperty(MULE_PROXY_ADDRESS, httpServerConnection.getRemoteClientAddress());
      }
    }
  }

  protected String processRelativePath(String contextPath, String path) {
    String relativePath = path.substring(contextPath.length());
    if (relativePath.startsWith("/")) {
      return relativePath.substring(1);
    }
    return relativePath;
  }

  @Override
  public Object acquireMessage() throws MuleException {
    final HttpRequest request;
    try {
      request = httpServerConnection.readRequest();
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
    if (request == null) {
      throw new HttpMessageReceiver.EmptyRequestException();
    }
    this.request = request;
    return request;
  }

  @Override
  public boolean validateMessage() {
    try {
      this.requestLine = httpServerConnection.getRequestLine();
      if (requestLine == null) {
        return false;
      }

      String method = requestLine.getMethod();

      if (!(method.equals(METHOD_GET)
          || method.equals(METHOD_HEAD)
          || method.equals(METHOD_POST)
          || method.equals(METHOD_OPTIONS)
          || method.equals(METHOD_PUT)
          || method.equals(METHOD_DELETE)
          || method.equals(METHOD_TRACE)
          || method.equals(METHOD_CONNECT)
          || method.equals(METHOD_PATCH))) {
        badRequest = true;
        return false;
      }
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  @Override
  public void discardInvalidMessage() throws MuleException {
    if (badRequest) {
      try {
        httpServerConnection.writeResponse(doBad(requestLine));
      } catch (IOException e) {
        throw new DefaultMuleException(e);
      }
    }
  }

  protected HttpResponse doBad(RequestLine requestLine) throws MuleException {
    MuleMessage message = getMessageReceiver().createMuleMessage(null);
    MuleEvent event = MuleEvent.builder(getMuleEvent().getContext()).message(message).flow(getFlowConstruct()).build();
    populateFieldsFromInboundEndpoint(event, getInboundEndpoint());
    setCurrentEvent(event);
    HttpResponse response = new HttpResponse();
    response.setStatusLine(requestLine.getHttpVersion(), SC_BAD_REQUEST);
    response.setBody(HttpMessages.malformedSyntax().toString() + CRLF);
    return transformResponse(response);
  }

  protected HttpServerConnection getHttpServerConnection() {
    return httpServerConnection;
  }

  @Override
  public void discardMessageOnThrottlingExceeded() throws MuleException {
    try {
      sendFailureResponseToClient(MESSAGE_DISCARD_STATUS_CODE, MESSAGE_THROTTLED_REASON_PHRASE);
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
  }

  @Override
  public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod,
                                            long timeUntilNextPeriodInMillis) {
    httpThrottlingHeadersMapBuilder.setThrottlingPolicyStatistics(remainingRequestInCurrentPeriod, maximumRequestAllowedPerPeriod,
                                                                  timeUntilNextPeriodInMillis);
  }

  private void sendFailureResponseToClient(int httpStatus, String message) throws IOException {
    httpServerConnection.writeFailureResponse(httpStatus, message, getThrottlingHeaders());
  }

  private void sendFailureResponseToClient(MessagingException exception, int httpStatus) throws IOException, MuleException {
    MuleEvent response = exception.getEvent();
    MuleMessage message = response.getMessage();
    httpStatus = message.getOutboundProperty(HTTP_STATUS_PROPERTY) != null
        ? Integer.valueOf(response.getMessage().getOutboundProperty(HTTP_STATUS_PROPERTY).toString()) : httpStatus;

    response.setMessage(MuleMessage.builder(response.getMessage())
        .payload(exception.getMessage())
        .addOutboundProperty(HTTP_STATUS_PROPERTY, httpStatus).build());
    HttpResponse httpResponse = transformResponse(response.getMessage());
    httpServerConnection.writeResponse(httpResponse, getThrottlingHeaders());
  }

  private Map<String, String> getThrottlingHeaders() {
    return httpThrottlingHeadersMapBuilder.build();
  }

  @Override
  public void messageProcessingEnded() {
    messageProcessedLatch.release();
  }


  public void awaitTermination() throws InterruptedException {
    this.messageProcessedLatch.await();
  }
}
