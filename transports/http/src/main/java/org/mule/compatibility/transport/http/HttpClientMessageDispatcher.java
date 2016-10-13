/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transformer.EndpointAwareTransformer;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.compatibility.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.TransformerChain;
import org.mule.runtime.core.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.BooleanUtils;

/**
 * <code>HttpClientMessageDispatcher</code> dispatches Mule events over HTTP.
 */
public class HttpClientMessageDispatcher extends AbstractMessageDispatcher implements MuleContextAware {

  /**
   * Range start for http error status codes.
   */
  public static final int ERROR_STATUS_CODE_RANGE_START = 400;
  public static final int REDIRECT_STATUS_CODE_RANGE_START = 300;
  protected final HttpConnector httpConnector;
  private volatile HttpClient client = null;
  private final Transformer sendTransformer;
  private MuleContext muleContext;

  public HttpClientMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
    this.httpConnector = (HttpConnector) endpoint.getConnector();
    List<Transformer> ts = httpConnector.getDefaultOutboundTransformers(null);
    if (ts.size() == 1) {
      this.sendTransformer = ts.get(0);
    } else if (ts.size() == 0) {
      this.sendTransformer = new ObjectToHttpClientMethodRequest();
      this.sendTransformer.setMuleContext(getEndpoint().getMuleContext());
      if (this.sendTransformer instanceof EndpointAwareTransformer) {
        ((EndpointAwareTransformer) this.sendTransformer).setEndpoint(endpoint);
      }
    } else {
      this.sendTransformer = new TransformerChain(ts);
    }
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    super.doInitialise();
    sendTransformer.initialise();
  }

  @Override
  protected void doConnect() throws Exception {
    if (client == null) {
      client = httpConnector.doClientConnect();
    }
  }

  @Override
  protected void doDisconnect() throws Exception {
    client = null;
  }

  @Override
  protected void doDispatch(Event event) throws Exception {
    HttpMethod httpMethod = getMethod(event);
    httpConnector.setupClientAuthorization(event, httpMethod, client, endpoint);

    try {
      execute(event, httpMethod);

      if (returnException(event, httpMethod)) {
        logger.error(httpMethod.getResponseBodyAsString());

        Exception cause = new Exception(String.format("Http call returned a status of: %1d %1s", httpMethod.getStatusCode(),
                                                      httpMethod.getStatusText()));
        throw new DispatchException(getEndpoint(), cause);
      } else if (httpMethod.getStatusCode() >= REDIRECT_STATUS_CODE_RANGE_START) {
        if (logger.isInfoEnabled()) {
          logger.info("Received a redirect response code: " + httpMethod.getStatusCode() + " " + httpMethod.getStatusText());
        }
      }
    } finally {
      httpMethod.releaseConnection();
    }
  }

  protected HttpMethod execute(Event event, HttpMethod httpMethod) throws Exception {
    // TODO set connection timeout buffer etc
    try {
      URI uri = endpoint.getEndpointURI().getUri();

      this.processCookies(event);
      this.processMuleSession(event, httpMethod);

      // TODO can we use the return code for better reporting?
      client.executeMethod(getHostConfig(uri), httpMethod);

      return httpMethod;
    } catch (IOException e) {
      // TODO employ dispatcher reconnection strategy at this point
      throw new DispatchException(getEndpoint(), e);
    } catch (Exception e) {
      throw new DispatchException(getEndpoint(), e);
    }

  }

  private void processMuleSession(Event event, HttpMethod httpMethod) {
    String muleSession = event.getMessage().getOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY);

    if (muleSession != null) {
      httpMethod.setRequestHeader(new Header(HttpConstants.HEADER_MULE_SESSION, muleSession));
    }
  }

  protected void processCookies(Event event) {
    InternalMessage msg = event.getMessage();

    Serializable cookiesProperty = msg.getInboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
    String cookieSpecProperty = (String) msg.getInboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
    processCookies(cookiesProperty, cookieSpecProperty, event);

    cookiesProperty = msg.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
    cookieSpecProperty = (String) msg.getOutboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
    processCookies(cookiesProperty, cookieSpecProperty, event);

    cookiesProperty = endpoint.getProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
    cookieSpecProperty = (String) endpoint.getProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
    processCookies(cookiesProperty, cookieSpecProperty, event);
  }

  private void processCookies(Object cookieObject, String policy, Event event) {
    URI uri = this.getEndpoint().getEndpointURI().getUri();
    CookieHelper.addCookiesToClient(this.client, cookieObject, policy, event, uri, muleContext);
  }

  protected HttpMethod getMethod(Event event) throws TransformerException {
    // Configure timeout. This is done here because MuleEvent.getTimeout() takes
    // precedence and is not available before send/dispatch.
    // Given that dispatchers are borrowed from a thread pool mutating client
    // here is ok even though it is not ideal.
    client.getHttpConnectionManager().getParams().setSoTimeout(endpoint.getResponseTimeout());

    event = setPropertyFromEndpoint(event, HttpConnector.HTTP_CUSTOM_HEADERS_MAP_PROPERTY);
    InternalMessage msg = event.getMessage();

    HttpMethod httpMethod;
    Object body = event.getMessage().getPayload().getValue();

    if (body instanceof HttpMethod) {
      httpMethod = (HttpMethod) body;
    } else {
      httpMethod = (HttpMethod) sendTransformer.transform(msg);
    }

    httpMethod.setFollowRedirects("true".equalsIgnoreCase((String) endpoint.getProperty("followRedirects")));

    // keepAlive=true is the default behavior of HttpClient
    if ("false".equalsIgnoreCase((String) endpoint.getProperty("keepAlive"))) {
      httpMethod.setRequestHeader("Connection", "close");
    }

    return httpMethod;
  }

  protected Event setPropertyFromEndpoint(Event event, String prop) {
    Serializable o = event.getMessage().getOutboundProperty(prop);
    if (o == null) {
      o = endpoint.getProperty(prop);
      if (o != null) {
        return Event.builder(event).message(InternalMessage.builder(event.getMessage()).addOutboundProperty(prop, o).build())
            .build();
      }
    }
    return event;
  }

  protected HttpMethod createEntityMethod(Event event, Object body, EntityEnclosingMethod postMethod)
      throws TransformerException {
    HttpMethod httpMethod;
    if (body instanceof String) {
      httpMethod = (HttpMethod) sendTransformer.transform(body.toString());
    } else if (body instanceof byte[]) {
      byte[] buffer = (byte[]) event.transformMessage(DataType.BYTE_ARRAY, muleContext);
      postMethod
          .setRequestEntity(new ByteArrayRequestEntity(buffer,
                                                       event.getMessage().getPayload().getDataType().getMediaType().getCharset()
                                                           .get().name()));
      httpMethod = postMethod;
    } else {
      if (!(body instanceof OutputHandler)) {
        body = event.transformMessage(DataType.fromType(OutputHandler.class), muleContext);
      }

      OutputHandler outputHandler = (OutputHandler) body;
      postMethod.setRequestEntity(new StreamPayloadRequestEntity(outputHandler, event));
      postMethod.setContentChunked(true);
      httpMethod = postMethod;
    }

    return httpMethod;
  }

  @Override
  protected InternalMessage doSend(Event event) throws Exception {
    HttpMethod httpMethod = getMethod(event);
    httpConnector.setupClientAuthorization(event, httpMethod, client, endpoint);

    httpMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new MuleHttpMethodRetryHandler());
    boolean releaseConn = false;
    try {
      httpMethod = execute(event, httpMethod);

      DefaultExceptionPayload ep = null;

      if (returnException(event, httpMethod)) {
        DispatchException exception = new DispatchException(getEndpoint(),
                                                            new HttpResponseException(httpMethod.getStatusText(),
                                                                                      httpMethod.getStatusCode()));
        ep = new DefaultExceptionPayload(exception);
      } else if (httpMethod.getStatusCode() >= REDIRECT_STATUS_CODE_RANGE_START) {
        try {
          return handleRedirect(httpMethod, event);
        } catch (Exception e) {
          DispatchException exception = new DispatchException(getEndpoint(), e);
          ep = new DefaultExceptionPayload(exception);
          return getResponseFromMethod(httpMethod, ep);
        }
      }
      releaseConn = httpMethod.getResponseBodyAsStream() == null;
      return getResponseFromMethod(httpMethod, ep);
    } catch (Exception e) {
      releaseConn = true;
      if (e instanceof DispatchException) {
        throw (DispatchException) e;
      }
      throw new DispatchException(getEndpoint(), e);
    } finally {
      if (releaseConn) {
        httpMethod.releaseConnection();
      }
    }
  }

  protected InternalMessage handleRedirect(HttpMethod method, Event event)
      throws HttpResponseException, MuleException, IOException {
    String followRedirects = (String) endpoint.getProperty("followRedirects");
    if (followRedirects == null || "false".equalsIgnoreCase(followRedirects)) {
      if (logger.isInfoEnabled()) {
        logger.info("Received a redirect, but followRedirects=false. Response code: " + method.getStatusCode() + " "
            + method.getStatusText());
      }
      return getResponseFromMethod(method, null);
    }
    Header locationHeader = method.getResponseHeader(HttpConstants.HEADER_LOCATION);
    if (locationHeader == null) {
      throw new HttpResponseException(method.getStatusText(), method.getStatusCode());
    }
    OutboundEndpoint out =
        new EndpointURIEndpointBuilder(locationHeader.getValue(), getEndpoint().getMuleContext()).buildOutboundEndpoint();
    Event result = out.process(event);
    if (result != null) {
      return result.getMessage();
    } else {
      return null;
    }
  }

  protected InternalMessage getResponseFromMethod(HttpMethod httpMethod, ExceptionPayload ep) throws IOException, MuleException {
    InternalMessage message = createMuleMessage(httpMethod);

    if (logger.isDebugEnabled()) {
      logger.debug("Http response is: " + message.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    return InternalMessage.builder(message).exceptionPayload(ep).build();
  }

  /**
   * An exception is thrown if http.status >= 400 and exceptions are not disabled through one of the following mechanisms in order
   * of precedence:
   *
   * - setting to true the flow variable "http.disable.status.code.exception.check" - setting to true the outbound property
   * "http.disable.status.code.exception.check" - setting to false the outbound endpoint attribute "exceptionOnMessageError"
   *
   * @return if an exception should be thrown
   */
  protected boolean returnException(Event event, HttpMethod httpMethod) {
    String disableCheck = event.getMessage().getOutboundProperty(HttpConnector.HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK);

    boolean throwException;
    if (disableCheck == null) {
      throwException = !"false".equals(endpoint.getProperty("exceptionOnMessageError"));
    } else {
      throwException = !BooleanUtils.toBoolean(disableCheck);
    }

    return httpMethod.getStatusCode() >= ERROR_STATUS_CODE_RANGE_START && throwException;
  }

  protected HostConfiguration getHostConfig(URI uri) throws Exception {
    Protocol protocol = Protocol.getProtocol(uri.getScheme().toLowerCase());

    String host = uri.getHost();
    int port = uri.getPort();
    HostConfiguration config = new HostConfiguration();
    config.setHost(host, port, protocol);
    if (StringUtils.isNotBlank(httpConnector.getProxyHostname())) {
      // add proxy support
      config.setProxy(httpConnector.getProxyHostname(), httpConnector.getProxyPort());
    }
    return config;
  }

  @Override
  protected void doDispose() {
    // template method
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
