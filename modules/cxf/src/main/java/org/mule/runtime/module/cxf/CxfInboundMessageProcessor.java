/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.core.message.DefaultEventBuilder.MuleEventImplementation.setCurrentEvent;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.module.cxf.support.DelegatingOutputStream;
import org.mule.runtime.module.cxf.transport.MuleUniversalDestination;
import org.mule.runtime.module.xml.stax.StaxSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.wsdl.http.AddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The CxfInboundMessageProcessor performs inbound CXF processing, sending an event through the CXF service, then on to the next
 * MessageProcessor. This processor gets built by a MessageProcessorBuilder which is responsible for configuring it and the
 * {@link Server} that it dispatches to.
 */
public class CxfInboundMessageProcessor extends AbstractInterceptingMessageProcessor implements Lifecycle, NonBlockingSupported {

  private static final String HTTP_REQUEST_PROPERTY_MANAGER_KEY = "_cxfHttpRequestPropertyManager";

  public static final String JMS_TRANSPORT = "jms";

  /**
   * logger used by this class
   */
  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected Bus bus;

  // manager to the component
  protected String transportClass;

  protected Server server;

  private boolean proxy;

  private QueryHandler wsdlQueryHandler;

  private MediaType mimeType;

  private HttpRequestPropertyManager requestPropertyManager;

  @Override
  public void initialise() throws InitialisationException {
    if (bus == null) {
      throw new InitialisationException(
                                        I18nMessageFactory
                                            .createStaticMessage("No CXF bus instance, this component has not been initialized properly."),
                                        this);
    }

    final HttpRequestPropertyManager httpRequestPropertyManager =
        muleContext.getRegistry().get(HTTP_REQUEST_PROPERTY_MANAGER_KEY);
    requestPropertyManager = httpRequestPropertyManager != null ? httpRequestPropertyManager : new HttpRequestPropertyManager();
  }

  @Override
  public void stop() throws MuleException {
    if (server != null) {
      server.stop();
    }
  }

  @Override
  public void start() throws MuleException {
    // Start the CXF Server
    if (server != null) {
      server.start();
    }
  }

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public Event process(Event event) throws MuleException {
    // if http request
    String requestPath = parseHttpRequestProperty(requestPropertyManager.getRequestPath(event.getMessage()));
    try {
      if (requestPath.indexOf('?') > -1) {
        return generateWSDLOrXSD(event, requestPath);
      } else {
        return sendToDestination(event);
      }
    } catch (IOException e) {
      throw new DefaultMuleException(e);
    }
  }

  private String parseHttpRequestProperty(String request) {
    String uriBase = "";

    if (!(request.contains("?wsdl")) && (!(request.contains("?xsd")))) {
      int qIdx = request.indexOf('?');
      if (qIdx > -1) {
        uriBase = request.substring(0, qIdx);
      }
    } else {
      uriBase = request;
    }

    return uriBase;
  }

  protected Event generateWSDLOrXSD(Event event, String req)
      throws IOException {
    // TODO: Is there a way to make this not so ugly?
    String ctxUri = requestPropertyManager.getBasePath(event.getMessage());
    String wsdlUri = getUri(event);
    String serviceUri = wsdlUri.substring(0, wsdlUri.indexOf('?'));

    EndpointInfo ei = getServer().getEndpoint().getEndpointInfo();

    if (serviceUri != null) {
      ei.setAddress(serviceUri);

      if (ei.getExtensor(AddressType.class) != null) {
        ei.getExtensor(AddressType.class).setLocation(serviceUri);
      }
    }

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    MediaType ct = null;

    if (wsdlQueryHandler.isRecognizedQuery(wsdlUri, ctxUri, ei)) {
      ct = parse(wsdlQueryHandler.getResponseContentType(wsdlUri, ctxUri));
      wsdlQueryHandler.writeResponse(wsdlUri, ctxUri, ei, out);
      out.flush();
    }

    String message;
    if (ct == null) {
      ct = TEXT;
      message = "No query handler found for URL.";
    } else {
      message = out.toString();
    }
    return Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload(message).mediaType(ct).build())
        .build();
  }

  private String getUri(Event event) {
    String scheme = requestPropertyManager.getScheme(event);
    String host = event.getMessage().getInboundProperty("Host");
    String ctx = requestPropertyManager.getRequestPath(event.getMessage());

    return scheme + "://" + host + ctx;
  }

  protected Event sendToDestination(Event event) throws MuleException, IOException {
    try {
      final Exchange exchange = new ExchangeImpl();

      final Event originalEvent = event;
      if (event.isAllowNonBlocking()) {
        final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();

        event = Event.builder(event).replyToHandler(new NonBlockingReplyToHandler() {

          @Override
          public Event processReplyTo(Event responseEvent, InternalMessage returnMessage, Object replyTo)
              throws MuleException {
            try {
              // CXF execution chain was suspended, so we need to resume it.
              // The MuleInvoker component will be recalled, by using the CxfConstants.NON_BLOCKING_RESPONSE flag we force using
              // the received response event instead of re-invoke the flow
              exchange.put(CxfConstants.MULE_EVENT, responseEvent);
              exchange.put(CxfConstants.NON_BLOCKING_RESPONSE, true);
              exchange.getInMessage().getInterceptorChain().resume();

              // Process the response
              responseEvent = (Event) exchange.get(CxfConstants.MULE_EVENT);
              responseEvent = processResponse(originalEvent, exchange, responseEvent);

              // Continue the non blocking execution
              originalReplyToHandler.processReplyTo(responseEvent, responseEvent.getMessage(), replyTo);
            } catch (Exception e) {
              ExceptionPayload exceptionPayload = new DefaultExceptionPayload(e);
              responseEvent = Event.builder(responseEvent).message(InternalMessage.builder(responseEvent.getMessage())
                  .exceptionPayload(exceptionPayload).addOutboundProperty(HTTP_STATUS_PROPERTY, 500).build()).build();
              processExceptionReplyTo(new MessagingException(responseEvent, e, CxfInboundMessageProcessor.this), replyTo);
            }
            return responseEvent;
          }

          @Override
          public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
            originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
          }
        }).build();
        // Update RequestContext ThreadLocal for backwards compatibility
        setCurrentEvent(event);
      }

      Event responseEvent = sendThroughCxf(event, exchange);

      if (responseEvent == null || !responseEvent.equals(NonBlockingVoidMuleEvent.getInstance())) {
        return processResponse(event, exchange, responseEvent);
      }

      return responseEvent;
    } catch (MuleException e) {
      logger.warn("Could not dispatch message to CXF!", e);
      throw e;
    }
  }

  private Event sendThroughCxf(Event event, Exchange exchange) throws TransformerException, IOException {
    final MessageImpl m = new MessageImpl();
    m.setExchange(exchange);
    final InternalMessage muleReqMsg = event.getMessage();
    String method = muleReqMsg.getInboundProperty(HTTP_METHOD_PROPERTY);

    MediaType ct = muleReqMsg.getDataType().getMediaType();
    if (!ct.matches(ANY)) {
      m.put(Message.CONTENT_TYPE, ct.toRfcString());
    }

    String path = requestPropertyManager.getRequestPath(event.getMessage());
    if (path == null) {
      path = "";
    }

    if (method != null) {
      m.put(Message.HTTP_REQUEST_METHOD, method);
      m.put(Message.PATH_INFO, path);
      String basePath = requestPropertyManager.getBasePath(muleReqMsg);
      m.put(Message.BASE_PATH, basePath);

      method = method.toUpperCase();
    }

    if (!"GET".equals(method)) {
      Object payload = event.getMessage().getPayload();

      setPayload(event, m, payload);
    }

    // TODO: Not sure if this is 100% correct - DBD
    String soapAction = getSoapAction(event.getMessage());
    m.put(SoapConstants.SOAP_ACTION_PROPERTY_CAPS, soapAction);

    org.apache.cxf.transport.Destination d;

    if (server != null) {
      d = server.getDestination();
    } else {
      String serviceUri = getUri(event);

      DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
      DestinationFactory df = dfm.getDestinationFactoryForUri(serviceUri);

      EndpointInfo ei = new EndpointInfo();
      ei.setAddress(serviceUri);
      d = df.getDestination(ei);
    }

    // For MULE-6829
    if (shouldSoapActionHeader()) {
      // Add protocol headers with the soap action so that the SoapActionInInterceptor can find them if it is soap v1.1
      Map<String, List<String>> protocolHeaders = new HashMap<>();
      if (soapAction != null && !soapAction.isEmpty()) {
        List<String> soapActions = new ArrayList<>();
        // An HTTP client MUST use [SOAPAction] header field when issuing a SOAP HTTP Request.
        // The header field value of empty string ("") means that the intent of the SOAP message is provided by the HTTP
        // Request-URI.
        // No value means that there is no indication of the intent of the message.
        soapActions.add(soapAction);
        protocolHeaders.put(SoapBindingConstants.SOAP_ACTION, soapActions);
      }

      if (SoapJMSConstants.SOAP_JMS_NAMESPACE.equals(((MuleUniversalDestination) d).getEndpointInfo().getTransportId())) {
        String contentType = muleReqMsg.getInboundProperty(SoapJMSConstants.CONTENTTYPE_FIELD);
        if (contentType == null) {
          contentType = "text/xml";
        }
        protocolHeaders.put(SoapJMSConstants.CONTENTTYPE_FIELD, Collections.singletonList(contentType));

        String requestUri = muleReqMsg.getInboundProperty(SoapJMSConstants.REQUESTURI_FIELD);
        if (requestUri == null) {
          if (muleReqMsg.getAttributes() instanceof HttpRequestAttributes) {
            final HttpRequestAttributes httpRequestAttributes = (HttpRequestAttributes) muleReqMsg.getAttributes();
            requestUri = httpRequestAttributes.getScheme() + "://" + httpRequestAttributes.getHeaders().get("host")
                + httpRequestAttributes.getRequestUri();
          } else {
            // TODO MULE-9705 remove this when http is fully migrated to the extension
            requestUri = muleReqMsg.getInboundProperty("http.scheme") + "://" + muleReqMsg.getInboundProperty("host")
                + muleReqMsg.getInboundProperty("http.request.uri");
          }
        }
        protocolHeaders.put(SoapJMSConstants.REQUESTURI_FIELD, Collections.singletonList(requestUri));
      }

      m.put(Message.PROTOCOL_HEADERS, protocolHeaders);
    }

    // Set up a listener for the response
    m.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
    m.setDestination(d);

    // mule will close the stream so don't let cxf, otherwise cxf will close it too early
    exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, Boolean.TRUE);
    exchange.setInMessage(m);

    // if there is a fault, then we need an event in here because we won't
    // have a responseEvent from the MuleInvoker
    exchange.put(CxfConstants.MULE_EVENT, event);

    // invoke the actual web service up until right before we serialize the
    // response
    try {
      d.getMessageObserver().onMessage(m);
    } catch (SuspendedInvocationException e) {
      Event responseEvent = (Event) exchange.get(CxfConstants.MULE_EVENT);

      if (responseEvent == null || !responseEvent.equals(NonBlockingVoidMuleEvent.getInstance())) {
        throw e;
      }
    }

    // get the response event
    return (Event) exchange.get(CxfConstants.MULE_EVENT);
  }

  private Event processResponse(Event event, Exchange exchange, Event responseEvent) {
    // If there isn't one, there was probably a fault, so use the original event
    if (responseEvent == null || VoidMuleEvent.getInstance().equals(responseEvent) || !event.getExchangePattern().hasResponse()) {
      return null;
    }

    final InternalMessage message = responseEvent.getMessage();
    InternalMessage.Builder builder = InternalMessage.builder(message);

    BindingOperationInfo binding = exchange.get(BindingOperationInfo.class);
    if (null != binding && null != binding.getOperationInfo() && binding.getOperationInfo().isOneWay()) {
      // For one-way operations, no envelope should be returned
      // (http://www.w3.org/TR/soap12-part2/#http-reqbindwaitstate)
      builder.addOutboundProperty(HTTP_STATUS_PROPERTY, ACCEPTED.getStatusCode());
      builder.nullPayload();
    } else {
      final Optional<Charset> charset = message.getDataType().getMediaType().getCharset();
      if (charset.isPresent()) {
        builder.payload(getResponseOutputHandler(exchange)).mediaType(XML.withCharset(charset.get()));
      } else {
        builder.payload(getResponseOutputHandler(exchange)).mediaType(XML);
      }
    }

    // Handle a fault if there is one.
    Message faultMsg = exchange.getOutFaultMessage();
    if (faultMsg != null) {
      if (null != binding && null != binding.getOperationInfo() && binding.getOperationInfo().isOneWay()) {
        final Optional<Charset> charset = message.getDataType().getMediaType().getCharset();
        if (charset.isPresent()) {
          builder.payload(getResponseOutputHandler(exchange)).mediaType(XML.withCharset(charset.get()));
        } else {
          builder.payload(getResponseOutputHandler(exchange)).mediaType(XML);
        }
      }
      Exception ex = faultMsg.getContent(Exception.class);
      if (ex != null) {
        builder.exceptionPayload(new DefaultExceptionPayload(ex));
        builder.addOutboundProperty(HTTP_STATUS_PROPERTY, 500);
      }
    }
    return Event.builder(responseEvent).message(builder.build()).build();
  }

  protected boolean shouldSoapActionHeader() {
    // Only add soap headers if we can validate the bindings. if not, cxf will throw a fault in SoapActionInInterceptor
    // we cannot validate the bindings if we're using mule's pass-through invoke proxy service
    boolean isGenericProxy = "http://support.cxf.module.runtime.mule.org/"
        .equals(getServer().getEndpoint().getService().getName().getNamespaceURI()) &&
        "ProxyService".equals(getServer().getEndpoint().getService().getName().getLocalPart());
    return !isGenericProxy;
  }

  @Override
  public Event processNext(Event event) throws MuleException {
    return super.processNext(event);
  }

  @Deprecated
  protected OutputHandler getRessponseOutputHandler(final MessageImpl m) {
    return getResponseOutputHandler(m);
  }

  protected OutputHandler getResponseOutputHandler(final MessageImpl m) {
    return getResponseOutputHandler(m.getExchange());
  }

  protected OutputHandler getResponseOutputHandler(final Exchange exchange) {
    OutputHandler outputHandler = (event, out) -> {
      Message outFaultMessage = exchange.getOutFaultMessage();
      Message outMessage = exchange.getOutMessage();

      Message contentMsg = null;
      if (outFaultMessage != null && outFaultMessage.getContent(OutputStream.class) != null) {
        contentMsg = outFaultMessage;
      } else if (outMessage != null) {
        contentMsg = outMessage;
      }

      if (contentMsg == null) {
        return;
      }

      DelegatingOutputStream delegate = contentMsg.getContent(DelegatingOutputStream.class);
      if (delegate.getOutputStream() instanceof ByteArrayOutputStream) {
        out.write(((ByteArrayOutputStream) delegate.getOutputStream()).toByteArray());
      }
      delegate.setOutputStream(out);

      out.flush();

      contentMsg.getInterceptorChain().resume();
    };
    return outputHandler;
  }

  private void setPayload(Event ctx, final MessageImpl m, Object payload) throws TransformerException {
    if (payload instanceof Reader) {
      m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader((Reader) payload));
    } else if (payload instanceof byte[]) {
      m.setContent(InputStream.class, new ByteArrayInputStream((byte[]) payload));
    } else if (payload instanceof StaxSource) {
      m.setContent(XMLStreamReader.class, ((StaxSource) payload).getXMLStreamReader());
    } else if (payload instanceof Source) {
      m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader((Source) payload));
    } else if (payload instanceof XMLStreamReader) {
      m.setContent(XMLStreamReader.class, payload);
    } else if (payload instanceof Document) {
      DOMSource source = new DOMSource((Node) payload);
      m.setContent(XMLStreamReader.class, StaxUtils.createXMLStreamReader(source));
    } else {
      ctx.getMessage().getDataType().getMediaType().getCharset().ifPresent(encoding -> {
        m.put(Message.ENCODING, encoding.name());
      });

      if (payload instanceof InputStream) {
        m.setContent(InputStream.class, payload);
      } else {
        InputStream is = (InputStream) ctx.transformMessage(DataType.INPUT_STREAM, muleContext);
        m.setContent(InputStream.class, is);
      }
    }
  }

  /**
   * Gets the stream representation of the current message.
   * 
   * @param context the event context
   * @return The inputstream for the current message
   * @throws MuleException
   */
  protected InputStream getMessageStream(Event context) throws MuleException {
    InputStream is;
    Object eventMsgPayload = context.getMessage().getPayload();

    if (eventMsgPayload instanceof InputStream) {
      is = (InputStream) eventMsgPayload;
    } else {
      is = (InputStream) context.transformMessage(DataType.INPUT_STREAM, muleContext);
    }
    return is;
  }

  protected String getSoapAction(InternalMessage message) {
    String action = message.getInboundProperty(SoapConstants.SOAP_ACTION_PROPERTY);

    if (action != null && action.startsWith("\"") && action.endsWith("\"") && action.length() >= 2) {
      action = action.substring(1, action.length() - 1);
    }

    return action;
  }

  public Bus getBus() {
    return bus;
  }

  public void setBus(Bus bus) {
    this.bus = bus;
  }

  public Server getServer() {
    return server;
  }

  public void setServer(Server server) {
    this.server = server;
  }

  public void setProxy(boolean proxy) {
    this.proxy = proxy;
  }

  public boolean isProxy() {
    return proxy;
  }

  public void setWSDLQueryHandler(QueryHandler wsdlQueryHandler) {
    this.wsdlQueryHandler = wsdlQueryHandler;
  }

  public MediaType getMimeType() {
    return mimeType;
  }

  public void setMimeType(MediaType mimeType) {
    this.mimeType = mimeType;
  }
}
