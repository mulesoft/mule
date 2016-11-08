/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import static org.apache.cxf.message.Message.MTOM_ENABLED;
import org.mule.extension.ws.api.SoapVersion;
import org.mule.extension.ws.internal.WebServiceConsumer;
import org.mule.extension.ws.internal.interceptor.NamespaceRestorerStaxInterceptor;
import org.mule.extension.ws.internal.interceptor.NamespaceSaverStaxInterceptor;
import org.mule.extension.ws.internal.interceptor.OutputMtomSoapAttachmentsInterceptor;
import org.mule.extension.ws.internal.interceptor.OutputSoapHeadersInterceptor;
import org.mule.extension.ws.internal.interceptor.SoapActionInterceptor;
import org.mule.extension.ws.internal.interceptor.StreamClosingInterceptor;
import org.mule.extension.ws.internal.transport.WscTransportFactory;
import org.mule.runtime.api.connection.ConnectionException;

import java.util.List;

import javax.wsdl.Port;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;

/**
 * Factory class that creates instances of cxf {@link Client}s.
 *
 * @since 4.0
 */
final class ClientFactory {

  private ClientFactory() {}

  static ClientFactory getInstance() {
    return new ClientFactory();
  }

  /**
   * Creates a new instance of a cxf {@link Client} for the given address ans soap version.
   * <p>
   * Adds all the custom {@link Interceptor}s to work with the {@link WebServiceConsumer}.
   *
   * @param address     the address of the web service
   * @param port        the configured port from the web service definition
   * @param soapVersion the soap version of the web service
   * @param mtomEnabled if should enable MTOM attachments or not.
   * @return a new configured {@link Client}.
   * @throws ConnectionException if the client couldn't be created.
   */
  Client create(String address, Port port, SoapVersion soapVersion, boolean mtomEnabled) throws ConnectionException {

    if (address == null) {
      address = getSoapAddress(port);
    }

    WscTransportFactory factory = new WscTransportFactory();
    Client client = factory.createClient(address, soapVersion.getVersion());

    client.getEndpoint().put(MTOM_ENABLED, mtomEnabled);

    // Request Interceptors
    client.getOutInterceptors().add(new SoapActionInterceptor());

    // Response Interceptors
    client.getInInterceptors().add(new NamespaceRestorerStaxInterceptor());
    client.getInInterceptors().add(new NamespaceSaverStaxInterceptor());
    client.getInInterceptors().add(new StreamClosingInterceptor());
    client.getInInterceptors().add(new CheckFaultInterceptor());
    client.getInInterceptors().add(new OutputSoapHeadersInterceptor());
    client.getInInterceptors().add(new SoapActionInterceptor());

    if (mtomEnabled) {
      client.getInInterceptors().add(new OutputMtomSoapAttachmentsInterceptor());
    }

    Binding binding = client.getEndpoint().getBinding();
    removeInterceptor(binding.getOutInterceptors(), WrappedOutInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap11FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap12FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), CheckFaultInterceptor.class.getName());

    return client;
  }

  private void removeInterceptor(List<Interceptor<? extends Message>> inInterceptors, String name) {
    inInterceptors.removeIf(i -> i instanceof PhaseInterceptor && ((PhaseInterceptor) i).getId().equals(name));
  }

  /**
   * Tries to find the address where the web service is located or fail.
   */
  private String getSoapAddress(Port port) throws ConnectionException {
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
}
