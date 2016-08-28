/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.transport;

import static org.apache.cxf.message.Message.DECOUPLED_CHANNEL_MESSAGE;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.DefaultMuleEvent;
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
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.module.cxf.CxfConfiguration;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.module.cxf.support.DelegatingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.logging.Logger;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * A Conduit is primarily responsible for sending messages from CXF to somewhere else. This conduit takes messages which are being
 * written and sends them to the Mule bus.
 */
public class MuleUniversalConduit extends AbstractConduit {

  private static final Logger LOGGER = LogUtils.getL7dLogger(MuleUniversalConduit.class);

  private EndpointInfo endpoint;

  protected CxfConfiguration configuration;

  private MuleUniversalTransport transport;

  private boolean closeInput = true;

  /**
   * @param ei The Endpoint being invoked by this destination.
   * @param t The EPR associated with this Conduit - i.e. the reply destination.
   */
  public MuleUniversalConduit(MuleUniversalTransport transport,
                              CxfConfiguration configuration,
                              EndpointInfo ei,
                              EndpointReferenceType t) {
    super(getTargetReference(ei, t));
    this.transport = transport;
    this.endpoint = ei;
    this.configuration = configuration;
  }

  @Override
  public void close(Message msg) throws IOException {
    OutputStream os = msg.getContent(OutputStream.class);
    if (os != null) {
      os.close();
    }

    if (closeInput) {
      InputStream in = msg.getContent(InputStream.class);
      if (in != null) {
        in.close();
      }
    }
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
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

    if (event == null || VoidMuleEvent.getInstance().equals(event) || decoupled) {
      // we've got an out of band WS-RM message or a message from a standalone client
      MuleMessage muleMsg = MuleMessage.builder().payload(handler).build();

      String url = setupURL(message);

      try {
        FlowConstruct flowConstruct = new FlowConstruct() {

          @Override
          public MuleContext getMuleContext() {
            return configuration.getMuleContext();
          }

          @Override
          public String getName() {
            return "MuleUniversalConduit";
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
        event = new DefaultMuleEvent(event == null ? create(flowConstruct, "MuleUniversalConduit") : event.getContext(), muleMsg,
                                     flowConstruct);
      } catch (Exception e) {
        throw new Fault(e);
      }
    } else {
      event.setMessage(MuleMessage.builder(event.getMessage()).payload(handler).mediaType(XML).build());
    }

    if (!decoupled) {
      message.getExchange().put(CxfConstants.MULE_EVENT, event);
    }
    message.put(CxfConstants.MULE_EVENT, event);

    final MuleEvent finalEvent = event;
    AbstractPhaseInterceptor<Message> i = new AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM) {

      @Override
      public void handleMessage(Message m) throws Fault {
        try {
          dispatchMuleMessage(m, finalEvent);
        } catch (MuleException e) {
          throw new Fault(e);
        }
      }
    };
    message.getInterceptorChain().add(i);
  }

  public String setupURL(Message message) throws MalformedURLException {
    String value = (String) message.get(Message.ENDPOINT_ADDRESS);
    String pathInfo = (String) message.get(Message.PATH_INFO);
    String queryString = (String) message.get(Message.QUERY_STRING);
    String username = (String) message.get(BindingProvider.USERNAME_PROPERTY);
    String password = (String) message.get(BindingProvider.PASSWORD_PROPERTY);

    String result = value != null ? value : getTargetOrEndpoint();

    if (username != null) {
      int slashIdx = result.indexOf("//");
      if (slashIdx != -1) {
        result = result.substring(0, slashIdx + 2) + username + ":" + password + "@" + result.substring(slashIdx + 2);
      }
    }

    // REVISIT: is this really correct?
    if (null != pathInfo && !result.endsWith(pathInfo)) {
      result = result + pathInfo;
    }
    if (queryString != null) {
      result = result + "?" + queryString;
    }
    return result;
  }

  protected void dispatchMuleMessage(final Message m, MuleEvent reqEvent) throws MuleException {
    try {
      if (reqEvent.isAllowNonBlocking()) {
        final ReplyToHandler originalReplyToHandler = reqEvent.getReplyToHandler();

        reqEvent = new DefaultMuleEvent(reqEvent, new NonBlockingReplyToHandler() {

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
        });
      }
      // Update RequestContext ThreadLocal for backwards compatibility
      setCurrentEvent(reqEvent);

      MuleEvent resEvent = processNext(reqEvent, m.getExchange());

      if (!resEvent.equals(NonBlockingVoidMuleEvent.getInstance())) {
        sendResultBackToCxf(m, resEvent);
      }
    } catch (MuleException me) {
      throw me;
    } catch (Exception e) {
      throw new DefaultMuleException(MessageFactory.createStaticMessage("Could not send message to Mule."), e);
    }
  }

  protected void sendResultBackToCxf(Message m, MuleEvent resEvent) throws TransformerException, IOException {
    if (resEvent != null && !VoidMuleEvent.getInstance().equals(resEvent)) {
      m.getExchange().put(CxfConstants.MULE_EVENT, resEvent);

      // If we have a result, send it back to CXF
      MuleMessage result = resEvent.getMessage();
      InputStream is = getResponseBody(m, resEvent);
      if (is != null) {
        DataType dataType;
        if (MediaType.ANY.matches(result.getDataType().getMediaType())) {
          dataType = DataType.builder(result.getDataType()).mediaType(MediaType.XML).build();
        } else {
          dataType = result.getDataType();
        }

        Message inMessage = new MessageImpl();
        dataType.getMediaType().getCharset().ifPresent(encoding -> {
          inMessage.put(Message.ENCODING, encoding.name());
        });
        inMessage.put(Message.CONTENT_TYPE, dataType.toString());
        inMessage.setContent(InputStream.class, is);
        inMessage.setExchange(m.getExchange());
        getMessageObserver().onMessage(inMessage);
        return;
      }
    }

    // No body in the response, mark the exchange as finished.
    m.getExchange().put(ClientImpl.FINISHED, Boolean.TRUE);
  }

  protected InputStream getResponseBody(Message m, MuleEvent result) throws TransformerException, IOException {
    boolean response = result != null
        && result.getMessage().getPayload() != null && !isOneway(m.getExchange());

    if (response) {
      // Sometimes there may not actually be a body, in which case
      // we want to act appropriately. E.g. one way invocations over a proxy
      InputStream is = (InputStream) configuration.getMuleContext().getTransformationService()
          .transform(result.getMessage(), DataType.INPUT_STREAM).getPayload();
      PushbackInputStream pb = new PushbackInputStream(is);
      result.setMessage(MuleMessage.builder(result.getMessage()).payload(pb).mediaType(XML).build());

      int b = pb.read();
      if (b != -1) {
        pb.unread(b);
        return pb;
      }
    }

    return null;
  }

  protected boolean isOneway(Exchange exchange) {
    return exchange != null && exchange.isOneWay();
  }

  protected String getTargetOrEndpoint() {
    if (target != null) {
      return target.getAddress().getValue();
    }

    return endpoint.getAddress();
  }

  public void onClose(final Message m) throws IOException {
    // template method
  }

  protected MuleEvent processNext(MuleEvent event, Exchange exchange) throws MuleException {
    CxfOutboundMessageProcessor processor =
        (CxfOutboundMessageProcessor) exchange.get(CxfConstants.CXF_OUTBOUND_MESSAGE_PROCESSOR);
    MuleEvent response;
    response = processor.processNext(event);

    Holder<MuleEvent> holder = (Holder<MuleEvent>) exchange.get("holder");
    holder.value = response;

    return response;
  }

  @Override
  public void close() {}

  /**
   * Get the target endpoint reference.
   * 
   * @param ei the corresponding EndpointInfo
   * @param t the given target EPR if available
   * @return the actual target
   */
  protected static EndpointReferenceType getTargetReference(EndpointInfo ei, EndpointReferenceType t) {
    EndpointReferenceType ref = null;
    if (null == t) {
      ref = new EndpointReferenceType();
      AttributedURIType address = new AttributedURIType();
      address.setValue(ei.getAddress());
      ref.setAddress(address);
      if (ei.getService() != null) {
        EndpointReferenceUtils.setServiceAndPortName(ref, ei.getService().getName(), ei.getName()
            .getLocalPart());
      }
    } else {
      ref = t;
    }
    return ref;
  }

  /**
   * Used to set appropriate message properties, exchange etc. as required for an incoming decoupled response (as opposed what's
   * normally set by the Destination for an incoming request).
   */
  protected class InterposedMessageObserver implements MessageObserver {

    /**
     * Called for an incoming message.
     * 
     * @param inMessage
     */
    @Override
    public void onMessage(Message inMessage) {
      // disposable exchange, swapped with real Exchange on correlation
      inMessage.setExchange(new ExchangeImpl());
      inMessage.put(DECOUPLED_CHANNEL_MESSAGE, Boolean.TRUE);
      inMessage.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_OK);
      inMessage.remove(Message.ASYNC_POST_RESPONSE_DISPATCH);

      incomingObserver.onMessage(inMessage);
    }
  }

  public void setCloseInput(boolean closeInput) {
    this.closeInput = closeInput;
  }

  protected CxfConfiguration getConnector() {
    return configuration;
  }

  protected EndpointInfo getEndpoint() {
    return endpoint;
  }

  protected MuleUniversalTransport getTransport() {
    return transport;
  }
}
