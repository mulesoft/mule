/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import static org.mule.compatibility.transport.http.HttpConnector.HTTP_STATUS_PROPERTY;
import static org.mule.compatibility.transport.http.HttpConstants.SC_NOT_ACCEPTABLE;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.transport.AbstractMessageReceiver;
import org.mule.compatibility.core.transport.TransportMessageProcessContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.util.MapUtils;

import java.util.List;

/**
 * <code>HttpMessageReceiver</code> is a simple http server that can be used to listen for HTTP requests on a particular port.
 */
public class HttpMessageReceiver extends AbstractMessageReceiver {

  public HttpMessageReceiver(Connector connector, FlowConstruct flowConstruct, InboundEndpoint endpoint) throws CreateException {
    super(connector, flowConstruct, endpoint);
  }

  @Override
  protected void doConnect() throws ConnectException {
    ((HttpConnector) connector).connect(endpoint.getEndpointURI());
  }

  @Override
  protected void doDisconnect() throws Exception {
    ((HttpConnector) connector).disconnect(endpoint.getEndpointURI());
  }

  HttpMessageProcessTemplate createMessageProcessTemplate(HttpServerConnection httpServerConnection) {
    return new HttpMessageProcessTemplate(this, httpServerConnection);
  }

  MessageProcessContext createMessageProcessContext() {
    return new TransportMessageProcessContext(this, getWorkManager());
  }

  void processRequest(HttpServerConnection httpServerConnection) throws InterruptedException, MuleException {
    HttpMessageProcessTemplate messageProcessTemplate = createMessageProcessTemplate(httpServerConnection);
    MessageProcessContext messageProcessContext = createMessageProcessContext();
    processMessage(messageProcessTemplate, messageProcessContext);
    messageProcessTemplate.awaitTermination();
  }

  protected String processRelativePath(String contextPath, String path) {
    String relativePath = path.substring(contextPath.length());
    if (relativePath.startsWith("/")) {
      return relativePath.substring(1);
    }
    return relativePath;
  }

  @Override
  protected void initializeMessageFactory() throws InitialisationException {
    HttpMuleMessageFactory factory;
    try {
      factory = (HttpMuleMessageFactory) super.createMuleMessageFactory();

      boolean enableCookies = MapUtils.getBooleanValue(endpoint.getProperties(), HttpConnector.HTTP_ENABLE_COOKIES_PROPERTY,
                                                       ((HttpConnector) connector).isEnableCookies());
      factory.setEnableCookies(enableCookies);

      String cookieSpec = MapUtils.getString(endpoint.getProperties(), HttpConnector.HTTP_COOKIE_SPEC_PROPERTY,
                                             ((HttpConnector) connector).getCookieSpec());
      factory.setCookieSpec(cookieSpec);

      factory.setExchangePattern(endpoint.getExchangePattern());

      muleMessageFactory = factory;
    } catch (CreateException ce) {
      Message message = MessageFactory.createStaticMessage(ce.getMessage());
      throw new InitialisationException(message, ce, this);
    }
  }

  @Override
  protected MuleMessage handleUnacceptedFilter(MuleMessage message) {
    if (logger.isDebugEnabled()) {
      logger.debug("Message request '" + message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY)
          + "' is being rejected since it does not match the filter on this endpoint: " + endpoint);
    }
    return MuleMessage.builder(message).addOutboundProperty(HTTP_STATUS_PROPERTY, String.valueOf(SC_NOT_ACCEPTABLE)).build();
  }

  public List<Transformer> getResponseTransportTransformers() {
    return this.defaultResponseTransformers;
  }

  public static class EmptyRequestException extends RuntimeException {

    public EmptyRequestException() {
      super();
    }
  }

  public static class FailureProcessingRequestException extends RuntimeException {

    public FailureProcessingRequestException() {
      super();
    }
  }
}
