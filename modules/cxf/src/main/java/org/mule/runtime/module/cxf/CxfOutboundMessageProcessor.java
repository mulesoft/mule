/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static java.util.Arrays.asList;
import static org.mule.extension.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METHOD_PROPERTY;
import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.Event.getVariableValueOrNull;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import static org.mule.runtime.module.cxf.CxfConstants.OPERATION;
import static org.mule.runtime.module.cxf.SoapConstants.SOAP_ACTION_PROPERTY;
import static org.mule.runtime.module.cxf.SoapConstants.SOAP_ACTION_PROPERTY_CAPS;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.CloneableMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.WrapperErrorMessageAwareException;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.module.cxf.i18n.CxfMessages;
import org.mule.runtime.module.cxf.security.WebServiceSecurityException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.WSAContextUtils;

/**
 * The CxfOutboundMessageProcessor performs outbound CXF processing, sending an event through the CXF client, then on to the next
 * MessageProcessor.
 */
public class CxfOutboundMessageProcessor extends AbstractInterceptingMessageProcessor
    implements CloneableMessageProcessor {

  private CxfPayloadToArguments payloadToArguments = CxfPayloadToArguments.NULL_PAYLOAD_AS_PARAMETER;
  private Client client;
  private boolean proxy;
  private String operation;
  private BindingProvider clientProxy;
  private String decoupledEndpoint;
  private MediaType mimeType;

  public CxfOutboundMessageProcessor(Client client) {
    this.client = client;
  }

  protected void cleanup() {
    // MULE-4899: cleans up client's request and response context to avoid a
    // memory leak.
    Map<String, Object> requestContext = client.getRequestContext();
    requestContext.clear();
    Map<String, Object> responseContext = client.getResponseContext();
    responseContext.clear();
  }

  protected Object[] getArgs(Event event) throws TransformerException {
    Object payload = event.getMessage().getPayload().getValue();

    if (proxy) {
      return new Object[] {event.getMessage()};
    }

    Object[] args = payloadToArguments.payloadToArrayOfArguments(payload);

    List<DataHandler> attachments = new ArrayList<>();
    for (String inboundAttachmentName : event.getMessage().getInboundAttachmentNames()) {
      attachments.add(event.getMessage().getInboundAttachment(inboundAttachmentName));
    }

    try {
      if (event.getMessage().getPayload().getValue() instanceof MultiPartPayload) {
        for (org.mule.runtime.api.message.Message part : ((MultiPartPayload) event.getMessage().getPayload().getValue())
            .getParts()) {
          attachments.add(toDataHandler(((PartAttributes) part.getAttributes()).getName(), part.getPayload().getValue(),
                                        part.getPayload().getDataType().getMediaType()));
        }
      }
    } catch (IOException e) {
      throw new TransformerException(createStaticMessage("Exception processing attachments."), e);
    }

    if (!attachments.isEmpty()) {
      List<Object> temp = new ArrayList<>(asList(args));
      temp.add(attachments.toArray(new DataHandler[attachments.size()]));
      args = temp.toArray();
    }

    if (args.length == 0) {
      return null;
    }
    return args;
  }

  @Override
  public Event process(Event event) throws MuleException {
    try {
      Event res;
      if (!isClientProxyAvailable()) {
        res = doSendWithClient(event);
      } else {
        res = doSendWithProxy(event);
      }
      return res;
    } finally {
      cleanup();
    }
  }

  private MuleException wrapException(Event event, Throwable ex) {
    if (ex instanceof MessagingException) {
      return (MessagingException) ex;
    }
    if (ex instanceof Fault) {
      // Because of CXF API, MuleExceptions can be wrapped in a Fault, in that case we should return the mule exception
      Fault fault = (Fault) ex;
      if (fault.getCause() instanceof MuleException) {
        MuleException muleException = (MuleException) fault.getCause();
        return muleException;
      }
      DispatchException dispatchException = new DispatchException(createStaticMessage(fault.getMessage()), this, fault);
      return new WrapperErrorMessageAwareException(event.getMessage(), dispatchException);
    }
    DispatchException dispatchException =
        new DispatchException(createStaticMessage(ExceptionHelper.getRootException(ex).getMessage()), this, ex);
    return new WrapperErrorMessageAwareException(event.getMessage(), dispatchException);
  }

  private MessagingException wrapToMessagingException(Event event, Throwable ex) {
    if (ex instanceof MessagingException) {
      return (MessagingException) ex;
    }
    if (ex instanceof Fault) {
      // Because of CXF API, MuleExceptions can be wrapped in a Fault, in that case we should return the mule exception
      Fault fault = (Fault) ex;
      if (fault.getCause() instanceof MuleException) {
        MuleException muleException = (MuleException) fault.getCause();
        return new MessagingException(event, muleException, this);
      }
      DispatchException dispatchException = new DispatchException(createStaticMessage(fault.getMessage()), this, fault);
      return new MessagingException(event, dispatchException);
    }
    DispatchException dispatchException =
        new DispatchException(createStaticMessage(ExceptionHelper.getRootException(ex).getMessage()), this, ex);
    return new MessagingException(event, dispatchException);
  }

  /**
   * This method is public so it can be invoked from the MuleUniversalConduit.
   */
  @Override
  public Event processNext(Event event) throws MuleException {
    return super.processNext(event);
  }

  protected Event doSendWithProxy(Event event) throws MuleException {
    try {
      Method method = getMethod(event);

      Map<String, Object> props = getInovcationProperties(event);

      Holder<Event> responseHolder = new Holder<>();
      props.put("holder", responseHolder);

      // Set custom soap action if set on the event or endpoint
      String soapAction = event.getMessage().getOutboundProperty(SOAP_ACTION_PROPERTY);
      if (soapAction != null) {
        props.put(SOAP_ACTION_PROPERTY_CAPS, soapAction);
      }

      clientProxy.getRequestContext().putAll(props);

      Object response;
      Object[] args = getArgs(event);
      try {
        response = method.invoke(clientProxy, args);
      } catch (InvocationTargetException e) {
        Throwable ex = e.getTargetException();

        if (ex != null && ex.getMessage().contains("Security")) {
          throw new WebServiceSecurityException(event, e, muleContext.getSecurityManager());
        } else {
          throw e;
        }
      }

      Object[] objResponse = addHoldersToResponse(response, args);
      Event muleRes = responseHolder.value;

      return buildResponseMessage(event, muleRes, objResponse);
    } catch (Exception e) {
      throw wrapException(event, e);
    }
  }

  protected Event doSendWithClient(final Event event) throws MuleException {
    BindingOperationInfo bop;
    try {
      bop = getOperation(event);
    } catch (Exception e) {
      throw wrapException(event, e);
    }

    Map<String, Object> props = getInovcationProperties(event);

    // Holds the response from the transport
    final Holder<Event> responseHolder = new Holder<>();
    props.put("holder", responseHolder);

    Map<String, Object> ctx = new HashMap<>();
    ctx.put(Client.REQUEST_CONTEXT, props);
    ctx.put(Client.RESPONSE_CONTEXT, props);

    // Set Custom Headers on the client
    Object[] arr = event.getMessage().getOutboundPropertyNames().toArray();
    String head;

    for (Object element : arr) {
      head = (String) element;
      if ((head != null) && (!head.startsWith("MULE"))) {
        props.put((String) element, event.getMessage().getOutboundProperty((String) element));
      }
    }

    ExchangeImpl exchange = new ExchangeImpl();
    // mule will close the stream so don't let cxf, otherwise cxf will close it too early
    exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, Boolean.TRUE);

    try {
      Object[] response = client.invoke(bop, getArgs(event), ctx, exchange);
      return buildResponseMessage(event, (Event) exchange.get(CxfConstants.MULE_EVENT), response);
    } catch (Exception e) {
      throw wrapException((Event) exchange.get(CxfConstants.MULE_EVENT), e);
    }
  }

  public Method getMethod(Event event) throws Exception {
    Method method = null;
    String opName = event.getMessage().getOutboundProperty(OPERATION);
    if (opName != null) {
      method = getMethodFromOperation(opName);
    }

    if (method == null) {
      opName = operation;
      if (opName != null) {
        method = getMethodFromOperation(opName);
      }
    }

    if (method == null) {
      throw new MessagingException(CxfMessages.noOperationWasFoundOrSpecified(), event, this);
    }
    return method;
  }

  protected BindingOperationInfo getOperation(final String opName) throws Exception {
    // Normally its not this hard to invoke the CXF Client, but we're
    // sending along some exchange properties, so we need to use a more advanced
    // method
    Endpoint ep = client.getEndpoint();
    BindingOperationInfo bop = getBindingOperationFromEndpoint(ep, opName);
    if (bop == null) {
      bop = tryToGetTheOperationInDotNetNamingConvention(ep, opName);
      if (bop == null) {
        throw new Exception("No such operation: " + opName);
      }
    }

    if (bop.isUnwrappedCapable()) {
      bop = bop.getUnwrappedOperation();
    }
    return bop;
  }

  /**
   * <p>
   * This method tries to call {@link #getBindingOperationFromEndpoint(Endpoint, String)} with the .NET naming convention for .NET
   * webservices (method names start with a capital letter).
   * </p>
   * <p>
   * CXF generates method names compliant with Java naming so if the WSDL operation names starts with uppercase letter, matching
   * with method name does not work - thus the work around.
   * </p>
   */
  protected BindingOperationInfo tryToGetTheOperationInDotNetNamingConvention(Endpoint ep, final String opName) {
    final String capitalizedOpName = opName.substring(0, 1).toUpperCase() + opName.substring(1);
    return getBindingOperationFromEndpoint(ep, capitalizedOpName);
  }

  protected BindingOperationInfo getBindingOperationFromEndpoint(Endpoint ep, final String operationName) {
    QName q = new QName(ep.getService().getName().getNamespaceURI(), operationName);
    BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
    return bop;
  }

  private Method getMethodFromOperation(String op) throws Exception {
    BindingOperationInfo bop = getOperation(op);
    MethodDispatcher md = (MethodDispatcher) client.getEndpoint().getService().get(MethodDispatcher.class.getName());
    return md.getMethod(bop);
  }

  protected String getMethodOrOperationName(Event event) throws DispatchException {
    // People can specify a CXF operation, which may in fact be different
    // than the method name. If that's not found, we'll default back to the
    // mule method property.
    String method = getVariableValueOrNull(OPERATION, event);
    if (method == null) {
      Object muleMethodProperty = getVariableValueOrNull(MULE_METHOD_PROPERTY, event);
      if (muleMethodProperty != null) {
        if (muleMethodProperty instanceof Method) {
          method = ((Method) muleMethodProperty).getName();
        } else {
          method = muleMethodProperty.toString();
        }
      }
    }

    if (method == null) {
      method = operation;
    }

    if (method == null && proxy) {
      return "invoke";
    }

    return method;
  }

  public BindingOperationInfo getOperation(Event event) throws Exception {
    String opName = getMethodOrOperationName(event);

    if (opName == null) {
      opName = operation;
    }

    return getOperation(opName);
  }

  private Map<String, Object> getInovcationProperties(Event event) {
    Map<String, Object> props = new HashMap<>();
    props.put(CxfConstants.MULE_EVENT, event);
    props.put(CxfConstants.CXF_OUTBOUND_MESSAGE_PROCESSOR, this);
    // props.put(org.apache.cxf.message.Message.ENDPOINT_ADDRESS, endpoint.getEndpointURI().toString());

    if (decoupledEndpoint != null) {
      props.put(WSAContextUtils.REPLYTO_PROPERTY, decoupledEndpoint);
    }

    return props;
  }

  protected Event buildResponseMessage(Event request, Event transportResponse, Object[] response) {
    // One way dispatches over an async transport result in this
    if (transportResponse == null) {
      return null;
    }

    // Otherwise we may have a response!
    Object payload;
    if (response == null || response.length == 0) {
      payload = null;
    } else if (response.length == 1) {
      payload = response[0];
    } else {
      payload = response;
    }

    InternalMessage.Builder builder = InternalMessage.builder(transportResponse.getMessage());
    Serializable httpStatusCode = transportResponse.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY);
    if (isProxy() && httpStatusCode != null) {
      builder.addOutboundProperty(HTTP_STATUS_PROPERTY, httpStatusCode);
    }

    builder.payload(payload).mediaType(getMimeType());

    return Event.builder(transportResponse).message(builder.build()).build();
  }

  protected Object[] addHoldersToResponse(Object response, Object[] args) {
    List<Object> responseWithHolders = new ArrayList<>();
    responseWithHolders.add(response);

    if (args != null) {
      for (Object arg : args) {
        if (arg instanceof Holder) {
          responseWithHolders.add(arg);
        }
      }
    }

    return responseWithHolders.toArray();
  }

  public void setPayloadToArguments(CxfPayloadToArguments payloadToArguments) {
    this.payloadToArguments = payloadToArguments;
  }

  protected boolean isClientProxyAvailable() {
    return clientProxy != null;
  }

  public boolean isProxy() {
    return proxy;
  }

  public void setProxy(boolean proxy) {
    this.proxy = proxy;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public void setClientProxy(BindingProvider clientProxy) {
    this.clientProxy = clientProxy;
  }

  public CxfPayloadToArguments getPayloadToArguments() {
    return payloadToArguments;
  }

  public Client getClient() {
    return client;
  }

  public void setDecoupledEndpoint(String decoupledEndpoint) {
    this.decoupledEndpoint = decoupledEndpoint;
  }

  @Override
  public Processor clone() {
    CxfOutboundMessageProcessor clone = new CxfOutboundMessageProcessor(client);
    clone.payloadToArguments = this.payloadToArguments;
    clone.proxy = this.proxy;
    clone.operation = this.operation;
    clone.clientProxy = clientProxy;
    clone.decoupledEndpoint = decoupledEndpoint;

    return clone;
  }

  public MediaType getMimeType() {
    return mimeType;
  }

  public void setMimeType(MediaType mimeType) {
    this.mimeType = mimeType;
  }
}
