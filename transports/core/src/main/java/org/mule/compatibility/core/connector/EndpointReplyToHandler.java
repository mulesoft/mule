/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.connector;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.config.i18n.TransportCoreMessages;
import org.mule.compatibility.core.transport.service.TransportFactory;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.connector.DefaultReplyToHandler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.Serializable;

public class EndpointReplyToHandler extends DefaultReplyToHandler {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  private static final int CACHE_MAX_SIZE = 1000;

  protected transient Connector connector;
  private transient LoadingCache<String, OutboundEndpoint> endpointCache;

  public EndpointReplyToHandler(MuleContext muleContext) {
    super(muleContext);
    endpointCache = buildCache(muleContext);
  }

  @Override
  public MuleEvent processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException {
    event = super.processReplyTo(event, returnMessage, replyTo);

    String replyToEndpoint = replyTo.toString();

    // Create a new copy of the message so that response MessageProcessors don't end up screwing up the reply
    returnMessage = MuleMessage.builder(returnMessage).payload(returnMessage.getPayload()).build();

    // Create the replyTo event asynchronous
    MuleEvent replyToEvent = MuleEvent.builder(event).message(returnMessage).build();

    // get the endpoint for this url
    OutboundEndpoint endpoint = getEndpoint(event, replyToEndpoint);

    // carry over properties
    final MuleMessage message = event.getMessage();
    final Builder builder = MuleMessage.builder(message);

    for (String propertyName : endpoint.getResponseProperties()) {
      Serializable propertyValue = message.getInboundProperty(propertyName);
      if (propertyValue != null) {
        builder.addOutboundProperty(propertyName, propertyValue);
      }
    }
    event.setMessage(builder.build());

    // dispatch the event
    try {
      if (logger.isInfoEnabled()) {
        logger.info("reply to sent: " + endpoint);
      }
      return endpoint.process(replyToEvent);
    } catch (Exception e) {
      throw new DispatchException(TransportCoreMessages.failedToDispatchToReplyto(endpoint), replyToEvent, endpoint, e);
    }
  }

  @Override
  public void initAfterDeserialisation(MuleContext context) throws MuleException {
    super.initAfterDeserialisation(context);

    connector = findConnector();
    endpointCache = buildCache(muleContext);
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  protected synchronized OutboundEndpoint getEndpoint(MuleEvent event, String endpointUri) throws MuleException {
    try {
      return endpointCache.get(endpointUri);
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  public Connector getConnector() {
    return connector;
  }

  protected Connector findConnector() {
    String connectorName = (String) serializedData.get("connectorName");
    String connectorType = (String) serializedData.get("connectorType");
    Connector found = null;

    if (connectorName != null) {
      found = muleContext.getRegistry().get(connectorName);
    } else if (connectorType != null) {
      found = new TransportFactory(muleContext).getDefaultConnectorByProtocol(connectorType);
    }
    return found;
  }

  private LoadingCache<String, OutboundEndpoint> buildCache(final MuleContext muleContext) {
    return CacheBuilder.newBuilder().maximumSize(CACHE_MAX_SIZE).<String, OutboundEndpoint>build(buildCacheLoader(muleContext));
  }

  private CacheLoader buildCacheLoader(final MuleContext muleContext) {
    return new CacheLoader<String, OutboundEndpoint>() {

      @Override
      public OutboundEndpoint load(String key) throws Exception {
        EndpointFactory endpointFactory = getEndpointFactory(muleContext.getRegistry());
        EndpointBuilder endpointBuilder = endpointFactory.getEndpointBuilder(key);
        return endpointFactory.getOutboundEndpoint(endpointBuilder);
      }
    };
  }

  public EndpointFactory getEndpointFactory(MuleRegistry registry) {
    return (EndpointFactory) registry.lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
