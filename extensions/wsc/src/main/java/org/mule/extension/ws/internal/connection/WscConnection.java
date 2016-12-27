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
import org.mule.extension.ws.api.security.SecurityStrategy;
import org.mule.extension.ws.internal.generator.attachment.AttachmentRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.AttachmentResponseEnricher;
import org.mule.extension.ws.internal.generator.attachment.MtomRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.MtomResponseEnricher;
import org.mule.extension.ws.internal.generator.attachment.SoapAttachmentRequestEnricher;
import org.mule.extension.ws.internal.generator.attachment.SoapAttachmentResponseEnricher;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.service.http.api.HttpService;

import java.util.List;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;

/**
 * A connection with a web service for consuming it's exposed resources.
 *
 * @since 4.0
 */
public class WscConnection {

  private static final ClientFactory clientFactory = ClientFactory.getInstance();

  private final WscClient client;
  private final WsdlIntrospecter wsdlIntrospecter;
  private final TypeLoader typeLoader;
  private final AttachmentRequestEnricher requestEnricher;
  private final AttachmentResponseEnricher responseEnricher;

  public WscConnection(String wsdlLocation,
                       String address,
                       String service,
                       String port,
                       SoapVersion version,
                       boolean mtomEnabled,
                       List<SecurityStrategy> securities,
                       HttpService httpService,
                       String transportConfig)
      throws ConnectionException {

    this.wsdlIntrospecter = new WsdlIntrospecter(wsdlLocation, service, port);

    if (wsdlIntrospecter.isRpcStyle()) {
      // TODO: MULE-11082
      throw new ConnectionException(format("The provided WSDL [%s] is RPC style, RPC WSDLs are not supported", wsdlLocation));
    }

    this.typeLoader = new XmlTypeLoader(wsdlIntrospecter.getSchemas());
    this.client = clientFactory.create(findAddress(address), version, mtomEnabled, securities, httpService, transportConfig);

    // TODO: MULE-10889 -> instead of creating this enrichers, interceptors that works with the live stream would be ideal
    if (mtomEnabled) {
      this.responseEnricher = new MtomResponseEnricher();
      this.requestEnricher = new MtomRequestEnricher();
    } else {
      this.responseEnricher = new SoapAttachmentResponseEnricher();
      this.requestEnricher = new SoapAttachmentRequestEnricher();
    }
  }

  /**
   * Invokes a Web Service Operation.
   */
  public Object[] invoke(String operation,
                         Object payload,
                         List<SoapHeader> headers,
                         List<Attachment> attachments,
                         String encoding,
                         Exchange exchange) {
    return client.invoke(operation, payload, headers, attachments, encoding, exchange);
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

  public AttachmentRequestEnricher getRequestEnricher() {
    return requestEnricher;
  }

  public AttachmentResponseEnricher getResponseEnricher() {
    return responseEnricher;
  }

  private String findAddress(String address) throws ConnectionException {
    if (address != null) {
      return address;
    }
    return wsdlIntrospecter.getSoapAddress()
        .orElseThrow(() -> new ConnectionException("No address was specified and no one was found for the given configuration"));
  }
}
