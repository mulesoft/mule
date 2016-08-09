/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.management.agent.mbean;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.util.TransportObjectNameHelper;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.config.i18n.CoreMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The EndpointServiceMBean allows you to check the confiugration of an endpoint and conect/disconnect endpoints manually.
 */
public class EndpointService implements EndpointServiceMBean {

  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private ImmutableEndpoint endpoint;
  private MessageReceiver receiver;
  private String name;
  private String componentName;

  public EndpointService(ImmutableEndpoint endpoint) {
    this.endpoint = endpoint;
    init();
  }

  public EndpointService(MessageReceiver receiver) {
    if (receiver == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("Receiver").getMessage());
    }
    this.endpoint = receiver.getEndpoint();
    this.receiver = receiver;
    this.componentName = receiver.getFlowConstruct().getName();
    init();
  }

  private void init() {
    if (endpoint == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("Endpoint").getMessage());
    }
    if (receiver == null && endpoint instanceof InboundEndpoint) {
      throw new IllegalArgumentException("Recevier is null for Endpoint MBean but the endpoint itself is a receiving endpoint");
    }

    name = new TransportObjectNameHelper(endpoint.getMuleContext()).getEndpointName(endpoint.getEndpointURI());
  }

  @Override
  public String getAddress() {
    return endpoint.getEndpointURI().getAddress();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isConnected() {
    return receiver == null || receiver.isConnected();
  }

  @Override
  public void connect() throws Exception {
    if (receiver != null && !receiver.isConnected()) {
      receiver.connect();
    } else if (logger.isDebugEnabled()) {
      logger.debug("Endpoint is already connected");
    }
  }

  @Override
  public void disconnect() throws Exception {
    if (receiver != null && receiver.isConnected()) {
      receiver.disconnect();
    } else if (logger.isDebugEnabled()) {
      logger.debug("Endpoint is already disconnected");
    }
  }

  @Override
  public boolean isInbound() {
    return endpoint instanceof InboundEndpoint;
  }

  @Override
  public boolean isOutbound() {
    return endpoint instanceof OutboundEndpoint;
  }

  @Override
  public MessageExchangePattern getMessageExchangePattern() {
    return endpoint.getExchangePattern();
  }

  @Override
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }
}
