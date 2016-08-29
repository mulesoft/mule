/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.transport;

import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.module.cxf.CxfConfiguration;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.module.cxf.support.DelegatingOutputStream;
import org.mule.runtime.module.cxf.transport.MuleUniversalConduit;
import org.mule.runtime.module.cxf.transport.MuleUniversalTransport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Holder;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class EndpointMuleUniversalConduit extends MuleUniversalConduit {

  private Map<String, OutboundEndpoint> endpoints = new HashMap<>();

  public EndpointMuleUniversalConduit(MuleUniversalTransport transport, CxfConfiguration configuration, EndpointInfo ei,
                                      EndpointReferenceType t) {
    super(transport, configuration, ei, t);
  }

  protected synchronized OutboundEndpoint getEndpoint(MuleContext muleContext, String uri) throws MuleException {
    if (endpoints.get(uri) != null) {
      return endpoints.get(uri);
    }

    OutboundEndpoint endpoint = getEndpointFactory(muleContext).getOutboundEndpoint(uri);
    endpoints.put(uri, endpoint);
    return endpoint;
  }

  private static EndpointFactory getEndpointFactory(MuleContext muleContext) {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }

  /**
   * Prepare the message for writing.
   */
  @Override
  public void prepare(final Message message) throws IOException {
    // save in a separate place in case we need to resend the request
    final ByteArrayOutputStream cache = new ByteArrayOutputStream();
    final DelegatingOutputStream delegating = new DelegatingOutputStream(cache);
    message.setContent(OutputStream.class, delegating);
    message.setContent(DelegatingOutputStream.class, delegating);

    OutputHandler handler = (event, out) -> {
      out.write(cache.toByteArray());

      delegating.setOutputStream(out);

      // resume writing!
      message.getInterceptorChain().doIntercept(message);
    };

    MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);
    // are we sending an out of band response for a server side request?
    boolean decoupled = event != null && message.getExchange().getInMessage() != null;

    OutboundEndpoint ep = null;

    if (event == null || VoidMuleEvent.getInstance().equals(event) || decoupled) {
      // we've got an out of band WS-RM message or a message from a standalone client
      MuleContext muleContext = configuration.getMuleContext();
      MuleMessage muleMsg = MuleMessage.builder().payload(handler).build();

      String url = setupURL(message);

      try {
        ep = getEndpoint(muleContext, url);
        FlowConstruct flowConstruct = new FlowConstruct() {

          @Override
          public MuleContext getMuleContext() {
            return muleContext;
          }

          @Override
          public String getName() {
            return "EndpointNotificationLoggerAgent";
          }

          @Override
          public LifecycleState getLifecycleState() {
            return null;
          }

          @Override
          public MessagingExceptionHandler getExceptionListener() {
            return null;
          }

          @Override
          public FlowConstructStatistics getStatistics() {
            return null;
          }
        };
        event = MuleEvent.builder(event == null ? DefaultMessageContext.create(flowConstruct, "EndpointMuleUniversalConduit")
            : event.getContext()).message(muleMsg).exchangePattern(ep.getExchangePattern()).flow(flowConstruct).build();
      } catch (Exception e) {
        throw new Fault(e);
      }
    } else {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload(handler).mediaType(MediaType.XML).build());
    }

    if (!decoupled) {
      message.getExchange().put(CxfConstants.MULE_EVENT, event);
    }
    message.put(CxfConstants.MULE_EVENT, event);

    final MuleEvent finalEvent = event;
    final OutboundEndpoint finalEndpoint = ep;
    AbstractPhaseInterceptor<Message> i = new AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM) {

      @Override
      public void handleMessage(Message m) throws Fault {
        try {
          dispatchMuleMessage(m, finalEvent, finalEndpoint);
        } catch (MuleException e) {
          throw new Fault(e);
        }
      }
    };
    message.getInterceptorChain().add(i);
  }

  protected void dispatchMuleMessage(final Message m, MuleEvent reqEvent, OutboundEndpoint endpoint) throws MuleException {
    try {
      reqEvent.setMessage(MuleMessage.builder(reqEvent.getMessage())
          .addOutboundProperty(HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK, Boolean.TRUE.toString())
          .build());

      if (reqEvent.isAllowNonBlocking()) {
        final ReplyToHandler originalReplyToHandler = reqEvent.getReplyToHandler();

        reqEvent = MuleEvent.builder(reqEvent).replyToHandler(new NonBlockingReplyToHandler() {

          @Override
          public MuleEvent processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException {
            try {
              Holder<MuleEvent> holder = (Holder<MuleEvent>) m.getExchange().get("holder");
              holder.value = event;
              sendResultBackToCxf(m, event);
            } catch (IOException e) {
              processExceptionReplyTo(new MessagingException(event, e), replyTo);
            }
            return event;
          }

          @Override
          public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
            originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
          }
        }).build();
      }
      // Update RequestContext ThreadLocal for backwards compatibility
      setCurrentEvent(reqEvent);

      MuleEvent resEvent = processNext(reqEvent, m.getExchange(), endpoint);

      if (!resEvent.equals(NonBlockingVoidMuleEvent.getInstance())) {
        sendResultBackToCxf(m, resEvent);
      }
    } catch (MuleException me) {
      throw me;
    } catch (Exception e) {
      throw new DefaultMuleException(MessageFactory.createStaticMessage("Could not send message to Mule."), e);
    }
  }

  protected MuleEvent processNext(MuleEvent event,
                                  Exchange exchange, OutboundEndpoint endpoint)
      throws MuleException {
    CxfOutboundMessageProcessor processor =
        (CxfOutboundMessageProcessor) exchange.get(CxfConstants.CXF_OUTBOUND_MESSAGE_PROCESSOR);
    MuleEvent response;
    if (processor == null) {
      response = endpoint.process(event);
    } else {
      response = processor.processNext(event);

      Holder<MuleEvent> holder = (Holder<MuleEvent>) exchange.get("holder");
      holder.value = response;
    }

    // response = processor.processNext(event);
    //
    // Holder<MuleEvent> holder = (Holder<MuleEvent>) exchange.get("holder");
    // holder.value = response;
    return response;
  }

}
