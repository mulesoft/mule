/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.client;

import static java.lang.String.format;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.service.http.api.HttpService;
import org.mule.services.soap.api.client.MessageDispatcher;
import org.mule.services.soap.api.client.SoapClient;
import org.mule.services.soap.api.client.SoapClientConfiguration;
import org.mule.services.soap.api.client.SoapClientFactory;
import org.mule.services.soap.introspection.WsdlIntrospecter;
import org.mule.services.soap.transport.DefaultHttpMessageDispatcher;

import org.apache.cxf.endpoint.Client;

/**
 * {@link SoapClientFactory} implementation that creates {@link SoapCxfClient} instances.
 *
 * @since 4.0
 */
public class SoapCxfClientFactory implements SoapClientFactory {

  private final HttpService httpService;

  public SoapCxfClientFactory(HttpService httpService) {
    this.httpService = httpService;
  }

  /**
   * Creates a new instance of a {@link SoapCxfClient} for the given address ans soap version.
   *
   * @throws ConnectionException if the client couldn't be created.
   */
  @Override
  public SoapClient create(SoapClientConfiguration config) throws ConnectionException {
    WsdlIntrospecter introspecter = getIntrospecter(config);
    XmlTypeLoader xmlTypeLoader = new XmlTypeLoader(introspecter.getSchemas());
    Client client = CxfClientProvider.getClient(config);
    MessageDispatcher dispatcher =
        createDispatcher(config.getAddress() != null ? config.getAddress() : findAddress(introspecter));
    return new SoapCxfClient(client,
                             introspecter,
                             xmlTypeLoader,
                             dispatcher,
                             config.getVersion(),
                             config.isMtomEnabled());
  }

  private WsdlIntrospecter getIntrospecter(SoapClientConfiguration config) throws ConnectionException {
    String wsdlLocation = config.getWsdlLocation();
    WsdlIntrospecter introspecter = new WsdlIntrospecter(wsdlLocation, config.getService(), config.getPort());
    if (introspecter.isRpcStyle()) {
      // TODO: MULE-11082  Support RPC Style - CXF DOES NOT SUPPORT RPC, if supported a new RPC Client should be created.
      throw new ConnectionException(format("The provided WSDL [%s] is RPC style, RPC WSDLs are not supported", wsdlLocation));
    }
    return introspecter;
  }


  // TODO: MULE-10783: use custom transport configuration
  private MessageDispatcher createDispatcher(String address) throws ConnectionException {
    String protocol = address.substring(0, address.indexOf("://"));
    if (protocol.startsWith("http")) {
      return DefaultHttpMessageDispatcher.create(address, httpService);
    }
    throw new IllegalArgumentException(format("cannot create a dispatcher for protocol [%s]", protocol));
  }

  private String findAddress(WsdlIntrospecter wsdlIntrospecter) throws ConnectionException {
    return wsdlIntrospecter.getSoapAddress()
        .orElseThrow(() -> new ConnectionException("No address was specified and no one was found for the given configuration"));
  }
}
