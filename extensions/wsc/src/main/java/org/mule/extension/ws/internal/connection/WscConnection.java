/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import static java.lang.String.format;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.ws.api.SoapVersion;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.generator.attachment.AttachmentRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.AttachmentResponseEnricher;
import org.mule.extension.ws.internal.generator.attachment.MtomRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.MtomResponseEnricher;
import org.mule.extension.ws.internal.generator.attachment.SoapAttachmentRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.SoapAttachmentResponseEnricher;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.extension.ws.api.security.SecurityStrategy;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * A connection with a web service for consuming it's exposed resources.
 *
 * @since 4.0
 */
public class WscConnection {

  private static ClientFactory clientFactory = ClientFactory.getInstance();

  private final Client client;
  private final WsdlIntrospecter wsdlIntrospecter;
  private final TypeLoader typeLoader;
  private final boolean mtomEnabled;
  private final AttachmentRequestEnricher requestEnricher;
  private final AttachmentResponseEnricher responseEnricher;

  public WscConnection(String wsdlLocation,
                       String address,
                       String service,
                       String port,
                       SoapVersion soapVersion,
                       List<SecurityStrategy> securityStrategies,
                       boolean mtomEnabled)
      throws ConnectionException {
    this.wsdlIntrospecter = new WsdlIntrospecter(wsdlLocation, service, port);

    if (wsdlIntrospecter.isRpcStyle()) {
      // TODO: MULE-11082
      throw new ConnectionException(format("The provided WSDL [%s] is RPC style, RPC WSDLs are not supported", wsdlLocation));
    }

    this.typeLoader = new XmlTypeLoader(this.wsdlIntrospecter.getSchemas());
    this.mtomEnabled = mtomEnabled;
    this.client = clientFactory.create(address, wsdlIntrospecter.getPort(), soapVersion, securityStrategies, mtomEnabled);

    if (mtomEnabled) {
      this.responseEnricher = new MtomResponseEnricher();
      this.requestEnricher = new MtomRequestEnricher();
    } else {
      this.responseEnricher = new SoapAttachmentResponseEnricher();
      this.requestEnricher = new SoapAttachmentRequestEnricher();
    }

  }

  public Object[] invoke(String operation, Object payload, Map<String, Object> ctx, Exchange exchange) {
    try {
      return client.invoke(getInvocationOperation(), new Object[] {payload}, ctx, exchange);
    } catch (SoapFault sf) {
      throw sf;
    } catch (Exception e) {
      throw new WscException(format("An unexpected error occur while consuming the [%s] web service operation", operation), e);
    }
  }

  public void disconnect() {
    client.destroy();
  }

  public ConnectionValidationResult validateConnection() {
    // TODO: MULE-10783 - add validation request with the http requester config. Maybe hit the "?wsdl" uri.
    return success();
  }

  public WsdlIntrospecter getWsdlIntrospecter() {
    return wsdlIntrospecter;
  }

  public TypeLoader getTypeLoader() {
    return typeLoader;
  }

  public boolean isMtomEnabled() {
    return mtomEnabled;
  }

  public AttachmentRequestEnricher getRequestEnricher() {
    return requestEnricher;
  }

  public AttachmentResponseEnricher getResponseEnricher() {
    return responseEnricher;
  }

  private BindingOperationInfo getInvocationOperation() throws Exception {
    // Normally its not this hard to invoke the CXF Client, but we're
    // sending along some exchange properties, so we need to use a more advanced
    // method
    Endpoint ep = client.getEndpoint();
    // The operation is always named invoke because hits our ProxyService implementation.
    QName q = new QName(ep.getService().getName().getNamespaceURI(), "invoke");
    BindingOperationInfo bop = ep.getBinding().getBindingInfo().getOperation(q);
    if (bop.isUnwrappedCapable()) {
      bop = bop.getUnwrappedOperation();
    }
    return bop;
  }
}
