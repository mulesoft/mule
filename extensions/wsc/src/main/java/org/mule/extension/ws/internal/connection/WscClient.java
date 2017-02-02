/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.extension.ws.api.SoapVersion.SOAP12;
import org.mule.extension.ws.api.SoapVersion;
import org.mule.extension.ws.api.exception.BadRequestException;
import org.mule.extension.ws.api.exception.SoapFaultException;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.transport.WscDispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * A client implementation that uses a cxf {@link Client} underneath, it connects to a Soap Server to perform requests to it.
 * <p>
 * It also defines some constants that are used to pass information between different cxf interceptors.
 *
 * @since 4.0
 */
public class WscClient {

  public static final String WSC_DISPATCHER = "mule.wsc.dispatcher";
  public static final String MULE_ATTACHMENTS_KEY = "mule.wsc.attachments";
  public static final String MULE_HEADERS_KEY = "mule.wsc.headers";
  public static final String MULE_SOAP_ACTION = "mule.wsc.soap.action";
  public static final String MULE_WSC_ENCODING = "mule.wsc.encoding";

  private final Client cxfClient;
  private final WscDispatcher dispatcher;
  private final SoapVersion soapVersion;
  private final boolean mtomEnabled;

  public WscClient(Client client, WscDispatcher dispatcher, SoapVersion soapVersion, boolean mtomEnabled) {
    this.soapVersion = soapVersion;
    this.mtomEnabled = mtomEnabled;
    this.cxfClient = client;
    this.dispatcher = dispatcher;
  }

  /**
   * Invokes a Web Service Operation with the specified parameters.
   *
   * @param operation   the operation that is going to be invoked.
   * @param payload     the request body to be bounded in the envelope.
   * @param headers     the request headers to be bounded in the envelope.
   * @param attachments the set of attachments that aims to be sent with the request.
   * @param encoding    the encoding of the message.
   * @param exchange    the exchange instance that will carry all the parameters when intercepting the message.
   */
  public Object[] invoke(String operation,
                         Object payload,
                         List<SoapHeader> headers,
                         List<Attachment> attachments,
                         String encoding,
                         Exchange exchange) {
    try {
      BindingOperationInfo bop = getInvocationOperation();
      Map<String, Object> ctx = getInvocationContext(operation, encoding, headers, attachments);
      return cxfClient.invoke(bop, new Object[] {payload}, ctx, exchange);
    } catch (SoapFault sf) {
      throw new SoapFaultException(sf);
    } catch (Fault f) {
      if (f.getMessage().contains("COULD_NOT_READ_XML")) {
        throw new BadRequestException(
                                      format("Error consuming the operation [%s], the request body is not a valid XML",
                                             operation));
      }
      throw new SoapFaultException(f);
    } catch (Exception e) {
      throw new WscException(format("An unexpected error occur while consuming the [%s] web service operation", operation), e);
    }
  }

  public void destroy() {
    cxfClient.destroy();
    dispatcher.dispose();
  }

  private BindingOperationInfo getInvocationOperation() throws Exception {
    // Normally its not this hard to invoke the CXF Client, but we're
    // sending along some exchange properties, so we need to use a more advanced
    // method
    Endpoint ep = cxfClient.getEndpoint();
    // The operation is always named invoke because hits our ProxyService implementation.
    QName q = new QName(ep.getService().getName().getNamespaceURI(), "invoke");
    BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
    if (bop.isUnwrappedCapable()) {
      bop = bop.getUnwrappedOperation();
    }
    return bop;
  }

  private Map<String, Object> getInvocationContext(String operation,
                                                   String encoding,
                                                   List<SoapHeader> headers,
                                                   List<Attachment> attachments) {
    Map<String, Object> props = new HashMap<>();

    if (mtomEnabled) {
      props.put(MULE_ATTACHMENTS_KEY, attachments);
    } else {
      // is NOT mtom the attachments must not be touched by cxf, we create a custom request embedding the attachment in the xml
      props.put(MULE_ATTACHMENTS_KEY, emptyList());
    }

    props.put(MULE_WSC_ENCODING, encoding);
    props.put(MULE_HEADERS_KEY, headers);

    if (soapVersion == SOAP12) {
      props.put(MULE_SOAP_ACTION, operation);
    }

    props.put(WSC_DISPATCHER, dispatcher);

    Map<String, Object> ctx = new HashMap<>();
    ctx.put(Client.REQUEST_CONTEXT, props);
    return ctx;
  }

  Client getCxfClient() {
    return cxfClient;
  }
}
