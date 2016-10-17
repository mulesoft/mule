/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.ws.api.interceptor.NamespaceRestorerStaxInterceptor;
import org.mule.extension.ws.api.interceptor.NamespaceSaverStaxInterceptor;
import org.mule.extension.ws.api.interceptor.OutputSoapHeadersInterceptor;
import org.mule.extension.ws.api.interceptor.StreamClosingInterceptor;
import org.mule.extension.ws.api.introspection.WsdlIntrospecter;
import org.mule.extension.ws.api.transport.WscTransportFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.util.List;
import java.util.Map;

import javax.wsdl.Port;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.service.model.BindingOperationInfo;

public class WscConnection {

  private final Client client;
  private final String wsdlLocation;
  private final SoapVersion soapVersion;
  private final WsdlIntrospecter wsdlIntrospecter;
  private final String address;

  public WscConnection(String wsdlLocation,
                       String address,
                       String serviceName,
                       String portName,
                       SoapVersion soapVersion)
      throws ConnectionException {
    this.wsdlIntrospecter = new WsdlIntrospecter(wsdlLocation, serviceName, portName);
    this.wsdlLocation = wsdlLocation;
    this.soapVersion = soapVersion;
    if (address == null) {
      address = getSoapAddress();
    }
    this.address = address;
    this.client = createClient(address, soapVersion);
  }

  public Object[] invoke(Object payload, Map<String, Object> ctx, Exchange exchange) throws Exception {
    return client.invoke(getOperation(), new Object[] {payload}, ctx, exchange);
  }

  public void addOutInterceptor(Interceptor<? extends Message> i) {
    client.getOutInterceptors().add(i);
  }

  public void addInInterceptor(Interceptor<? extends Message> i) {
    client.getInInterceptors().add(i);
  }

  public void disconnect() {
    client.destroy();
  }

  public ConnectionValidationResult validateConnection() {
    // TODO: add validation request with the http requester config.
    return success();
  }

  private BindingOperationInfo getOperation() throws Exception {
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

  private Client createClient(String address, SoapVersion soapVersion) {
    WscTransportFactory factory = new WscTransportFactory();
    Client client = factory.createClient(address, soapVersion.getVersion());

    // Response Interceptors
    client.getInInterceptors().add(new NamespaceRestorerStaxInterceptor());
    client.getInInterceptors().add(new NamespaceSaverStaxInterceptor());
    client.getInInterceptors().add(new StreamClosingInterceptor());
    client.getInInterceptors().add(new CheckFaultInterceptor());
    client.getInInterceptors().add(new OutputSoapHeadersInterceptor());

    //client.getInInterceptors().add(new CopyAttachmentInInterceptor());
    //client.getOutInterceptors().add(new CopyAttachmentOutInterceptor());

    Binding binding = client.getEndpoint().getBinding();
    removeInterceptor(binding.getOutInterceptors(), WrappedOutInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap11FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap12FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), CheckFaultInterceptor.class.getName());

    return client;
  }

  private void removeInterceptor(List<Interceptor<? extends Message>> inInterceptors, String name) {
    inInterceptors.stream()
        .filter(i -> i instanceof PhaseInterceptor)
        .filter(i -> ((PhaseInterceptor) i).getId().equals(name))
        .findFirst()
        .ifPresent(inInterceptors::remove);
  }

  private String getSoapAddress() throws ConnectionException {
    Port port = wsdlIntrospecter.getPort();
    if (port != null) {
      for (Object address : port.getExtensibilityElements()) {
        if (address instanceof SOAPAddress) {
          return ((SOAPAddress) address).getLocationURI();
        } else if (address instanceof SOAP12Address) {
          return ((SOAP12Address) address).getLocationURI();
        } else if (address instanceof HTTPAddress) {
          return ((HTTPAddress) address).getLocationURI();
        }
      }
    }
    throw new ConnectionException("Cannot create connection without an address, please specify one");
  }

  public String getWsdlLocation() {
    return wsdlLocation;
  }

  public SoapVersion getSoapVersion() {
    return soapVersion;
  }

  public WsdlIntrospecter getWsdlIntrospecter() {
    return wsdlIntrospecter;
  }
}
