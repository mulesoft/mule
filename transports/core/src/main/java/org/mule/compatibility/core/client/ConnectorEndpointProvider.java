/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.client;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupServiceDescriptor;

import org.mule.compatibility.core.api.client.LocalMuleClient;
import org.mule.compatibility.core.api.endpoint.EndpointCache;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.registry.LegacyServiceType;
import org.mule.compatibility.core.api.transport.ReceiveException;
import org.mule.compatibility.core.config.ConnectorConfiguration;
import org.mule.compatibility.core.endpoint.SimpleEndpointCache;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.RequestCacheKey;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.client.AbstractPriorizableConnectorMessageProcessorProvider;
import org.mule.runtime.core.client.DefaultLocalMuleClient.MuleClientFlowConstruct;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides transports support to {@link LocalMuleClient}.
 */
public class ConnectorEndpointProvider extends AbstractPriorizableConnectorMessageProcessorProvider {

  private EndpointCache endpointCache;
  private Set<String> supportedUrlSchemas = Collections.synchronizedSet(new HashSet<>());
  private Set<String> unsupportedUrlSchemas = Collections.synchronizedSet(new HashSet<>());

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    this.endpointCache = new SimpleEndpointCache(muleContext);
  }

  @Override
  public boolean supportsUrl(String url) {
    if (!url.contains(":")) {
      return true;
    }
    final String schema = url.substring(0, url.indexOf(':'));

    if (supportedUrlSchemas.contains(schema)) {
      return true;
    } else if (unsupportedUrlSchemas.contains(schema)) {
      return false;
    }

    synchronized (this) {
      try {
        lookupServiceDescriptor(muleContext.getRegistry(), LegacyServiceType.TRANSPORT, schema, null);
      } catch (ServiceException e) {
        unsupportedUrlSchemas.add(schema);
        return false;
      }

      supportedUrlSchemas.add(schema);
      return true;
    }
  }

  @Override
  protected MessageProcessor buildMessageProcessor(RequestCacheKey cacheKey) throws MuleException {
    if (cacheKey.getOperationOptions().isOutbound()) {
      return endpointCache.getOutboundEndpoint(cacheKey.getUrl(), cacheKey.getExchangePattern(), null);
    } else {
      final Long timeout = cacheKey.getOperationOptions().getResponseTimeout();
      return event -> {
        final InboundEndpoint inboundEndpoint =
            endpointCache.getInboundEndpoint(cacheKey.getUrl(), cacheKey.getExchangePattern());
        MuleMessage message;
        try {
          message = inboundEndpoint.request(timeout);
        } catch (Exception e) {
          throw new ReceiveException(inboundEndpoint, timeout, e);
        }
        MuleClientFlowConstruct flowConstruct = new MuleClientFlowConstruct(muleContext);
        return message != null ? MuleEvent.builder(event.getContext()).message(message).flow(flowConstruct).build() : null;
      };
    }
  }

  @Override
  public int priority() {
    return ConnectorConfiguration.useTransportForUris(muleContext) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
  }
}
