/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import org.mule.compatibility.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.endpoint.inbound.InboundEndpointMimeTypeCheckingMessageProcessor;
import org.mule.compatibility.core.endpoint.inbound.InboundEndpointPropertyMessageProcessor;
import org.mule.compatibility.core.endpoint.inbound.InboundExceptionDetailsMessageProcessor;
import org.mule.compatibility.core.endpoint.inbound.InboundLoggingMessageProcessor;
import org.mule.compatibility.core.endpoint.inbound.InboundNotificationMessageProcessor;
import org.mule.compatibility.core.endpoint.outbound.OutboundEndpointMimeTypeCheckingMessageProcessor;
import org.mule.compatibility.core.endpoint.outbound.OutboundEndpointPropertyMessageProcessor;
import org.mule.compatibility.core.endpoint.outbound.OutboundLoggingMessageProcessor;
import org.mule.compatibility.core.endpoint.outbound.OutboundRootMessageIdPropertyMessageProcessor;
import org.mule.compatibility.core.endpoint.outbound.OutboundSessionHandlerMessageProcessor;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.EndpointTransactionalInterceptingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.requestreply.ReplyToPropertyRequestReplyReplier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultEndpointMessageProcessorChainFactory implements EndpointMessageProcessorChainFactory {

  /** Override this method to change the default MessageProcessors. */
  protected List<Processor> createInboundMessageProcessors(InboundEndpoint endpoint) {
    List<Processor> list = new ArrayList<>();

    list.add(new InboundEndpointMimeTypeCheckingMessageProcessor(endpoint));
    list.add(new InboundEndpointPropertyMessageProcessor(endpoint));
    list.add(new InboundNotificationMessageProcessor(endpoint));
    list.add(new InboundLoggingMessageProcessor(endpoint));

    return list;
  }

  /** Override this method to change the default MessageProcessors. */
  protected List<Processor> createInboundResponseMessageProcessors(InboundEndpoint endpoint) {
    List<Processor> list = new ArrayList<>();

    list.add(new InboundExceptionDetailsMessageProcessor(endpoint.getConnector()));
    list.add(new ReplyToPropertyRequestReplyReplier());

    return list;
  }

  /** Override this method to change the default MessageProcessors. */
  protected List<Processor> createOutboundMessageProcessors(OutboundEndpoint endpoint) throws MuleException {
    Connector connector = endpoint.getConnector();

    List<Processor> list = new ArrayList<>();

    // Log but don't proceed if connector is not started
    list.add(new OutboundLoggingMessageProcessor());
    list.add(new ProcessIfStartedMessageProcessor(connector, connector.getLifecycleState()));

    // Everything is processed within ExecutionTemplate
    list.add(new EndpointTransactionalInterceptingMessageProcessor(endpoint.getTransactionConfig()));

    list.add(new OutboundSessionHandlerMessageProcessor(connector.getSessionHandler(), endpoint.getMuleContext()));
    list.add(new OutboundEndpointPropertyMessageProcessor(endpoint));
    list.add(new OutboundRootMessageIdPropertyMessageProcessor());
    list.add(new OutboundEndpointMimeTypeCheckingMessageProcessor(endpoint));

    return list;
  }

  /** Override this method to change the default MessageProcessors. */
  protected List<Processor> createOutboundResponseMessageProcessors(OutboundEndpoint endpoint) throws MuleException {
    return Collections.emptyList();
  }

  @Override
  public Processor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct,
                                                      Processor target)
      throws MuleException {
    // -- REQUEST CHAIN --
    DefaultMessageProcessorChainBuilder requestChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint);
    requestChainBuilder.setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' request chain");
    // Default MPs
    requestChainBuilder.chain(createInboundMessageProcessors(endpoint));
    // Configured MPs (if any)
    AbstractRedeliveryPolicy redeliveryPolicy = endpoint.getRedeliveryPolicy();
    if (redeliveryPolicy != null) {
      requestChainBuilder.chain(redeliveryPolicy);
    }
    requestChainBuilder.chain(endpoint.getMessageProcessors());

    // -- INVOKE FLOW --
    if (target == null) {
      throw new ConfigurationException(I18nMessageFactory
          .createStaticMessage("No listener (target) has been set for this endpoint"));
    }
    requestChainBuilder.chain(target);

    if (!endpoint.getExchangePattern().hasResponse()) {
      return requestChainBuilder.build();
    } else {
      // -- RESPONSE CHAIN --
      DefaultMessageProcessorChainBuilder responseChainBuilder =
          new EndpointMessageProcessorChainBuilder(endpoint);
      responseChainBuilder.setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' response chain");
      // Default MPs
      responseChainBuilder.chain(createInboundResponseMessageProcessors(endpoint));
      // Configured MPs (if any)
      responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

      // -- COMPOSITE REQUEST/RESPONSE CHAIN --
      // Compose request and response chains. We do this so that if the request
      // chain returns early the response chain is still invoked.
      DefaultMessageProcessorChainBuilder compositeChainBuilder =
          new EndpointMessageProcessorChainBuilder(endpoint);
      compositeChainBuilder
          .setName("InboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' composite request/response chain");
      compositeChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());
      return compositeChainBuilder.build();
    }
  }

  @Override
  public Processor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, Processor target)
      throws MuleException {
    // -- REQUEST CHAIN --
    DefaultMessageProcessorChainBuilder requestChainBuilder = new OutboundEndpointMessageProcessorChainBuilder(endpoint);
    requestChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' request chain");
    // Default MPs
    requestChainBuilder.chain(createOutboundMessageProcessors(endpoint));
    // Configured MPs (if any)
    requestChainBuilder.chain(endpoint.getMessageProcessors());
    requestChainBuilder.chain(target);

    // -- INVOKE MESSAGE DISPATCHER --
    if (target == null) {
      throw new ConfigurationException(I18nMessageFactory
          .createStaticMessage("No listener (target) has been set for this endpoint"));
    }

    if (!endpoint.getExchangePattern().hasResponse()) {
      return requestChainBuilder.build();
    } else {
      // -- RESPONSE CHAIN --
      DefaultMessageProcessorChainBuilder responseChainBuilder = new EndpointMessageProcessorChainBuilder(endpoint);
      responseChainBuilder.setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' response chain");
      // Default MPs
      responseChainBuilder.chain(createOutboundResponseMessageProcessors(endpoint));
      // Configured MPs (if any)
      responseChainBuilder.chain(endpoint.getResponseMessageProcessors());

      // Compose request and response chains. We do this so that if the request
      // chain returns early the response chain is still invoked.
      DefaultMessageProcessorChainBuilder compositeChainBuilder = new OutboundEndpointMessageProcessorChainBuilder(endpoint);
      compositeChainBuilder
          .setName("OutboundEndpoint '" + endpoint.getEndpointURI().getUri() + "' composite request/response chain");
      compositeChainBuilder.chain(requestChainBuilder.build(), responseChainBuilder.build());
      return compositeChainBuilder.build();
    }
  }
}

