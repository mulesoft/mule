/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring;

import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_DEFAULT_EXCHANGE_PATTERN;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_DISPATCHER_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_ENDPOINT_BUILDER;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_INBOUND_EXCHANGE_PATTERNS;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_INBOUND_TRANSFORMER;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_MESSAGE_FACTORY;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_OUTBOUND_TRANSFORMER;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_RESPONSE_TRANSFORMER;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_SESSION_HANDLER;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS;
import static org.mule.runtime.core.api.config.MuleProperties.CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS;
import static org.mule.runtime.core.api.config.MuleProperties.SERVICE_FINDER;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ObjectFactory} for transport service override configuration.
 *
 * It creates a {@code Map} with all the entries defined by service overrides.
 *
 * @since 4.0
 */
public class ServiceOverridesObjectFactory implements ObjectFactory<Map<String, String>> {

  private String messageReceiver;
  private String transactedMessageReceiver;
  private String xaTransactedMessageReceiver;
  private String dispatcherFactory;
  private String inboundTransformer;
  private String outboundTransformer;
  private String responseTransformer;
  private String endpointBuilder;
  private String messageFactory;
  private String serviceFinder;
  private String sessionHandler;
  private String inboundExchangePatterns;
  private String outboundExchangePatterns;
  private String defaultExchangePattern;

  public void setMessageReceiver(String messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  public void setTransactedMessageReceiver(String transactedMessageReceiver) {
    this.transactedMessageReceiver = transactedMessageReceiver;
  }

  public void setXaTransactedMessageReceiver(String xaTransactedMessageReceiver) {
    this.xaTransactedMessageReceiver = xaTransactedMessageReceiver;
  }

  public void setDispatcherFactory(String dispatcherFactory) {
    this.dispatcherFactory = dispatcherFactory;
  }

  public void setInboundTransformer(String inboundTransformer) {
    this.inboundTransformer = inboundTransformer;
  }

  public void setOutboundTransformer(String outboundTransformer) {
    this.outboundTransformer = outboundTransformer;
  }

  public void setResponseTransformer(String responseTransformer) {
    this.responseTransformer = responseTransformer;
  }

  public void setEndpointBuilder(String endpointBuilder) {
    this.endpointBuilder = endpointBuilder;
  }

  public void setMessageFactory(String messageFactory) {
    this.messageFactory = messageFactory;
  }

  public void setServiceFinder(String serviceFinder) {
    this.serviceFinder = serviceFinder;
  }

  public void setSessionHandler(String sessionHandler) {
    this.sessionHandler = sessionHandler;
  }

  public void setInboundExchangePatterns(String inboundExchangePatterns) {
    this.inboundExchangePatterns = inboundExchangePatterns;
  }

  public void setOutboundExchangePatterns(String outboundExchangePatterns) {
    this.outboundExchangePatterns = outboundExchangePatterns;
  }

  public void setDefaultExchangePattern(String defaultExchangePattern) {
    this.defaultExchangePattern = defaultExchangePattern;
  }

  @Override
  public Map<String, String> getObject() throws Exception {
    HashMap<String, String> overrides = new HashMap<>();
    putIfHasValue(overrides, CONNECTOR_MESSAGE_RECEIVER_CLASS, messageReceiver);
    putIfHasValue(overrides, CONNECTOR_TRANSACTED_MESSAGE_RECEIVER_CLASS, transactedMessageReceiver);
    putIfHasValue(overrides, CONNECTOR_XA_TRANSACTED_MESSAGE_RECEIVER_CLASS, xaTransactedMessageReceiver);
    putIfHasValue(overrides, CONNECTOR_DISPATCHER_FACTORY, dispatcherFactory);
    putIfHasValue(overrides, CONNECTOR_MESSAGE_FACTORY, messageFactory);
    putIfHasValue(overrides, CONNECTOR_INBOUND_TRANSFORMER, inboundTransformer);
    putIfHasValue(overrides, CONNECTOR_OUTBOUND_TRANSFORMER, outboundTransformer);
    putIfHasValue(overrides, CONNECTOR_RESPONSE_TRANSFORMER, responseTransformer);
    putIfHasValue(overrides, CONNECTOR_ENDPOINT_BUILDER, endpointBuilder);
    putIfHasValue(overrides, SERVICE_FINDER, serviceFinder);
    putIfHasValue(overrides, CONNECTOR_SESSION_HANDLER, sessionHandler);
    putIfHasValue(overrides, CONNECTOR_INBOUND_EXCHANGE_PATTERNS, inboundExchangePatterns);
    putIfHasValue(overrides, CONNECTOR_OUTBOUND_EXCHANGE_PATTERNS, outboundExchangePatterns);
    putIfHasValue(overrides, CONNECTOR_DEFAULT_EXCHANGE_PATTERN, defaultExchangePattern);
    return overrides;
  }

  public void putIfHasValue(Map<String, String> map, String key, String value) {
    if (value != null) {
      map.put(key, value);
    }
  }
}
