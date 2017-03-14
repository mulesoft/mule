/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.generator;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.services.soap.api.message.SoapAttachment;
import org.mule.services.soap.api.message.SoapResponse;
import org.mule.services.soap.impl.client.SoapCxfClient;
import org.mule.services.soap.impl.exception.BadResponseException;
import org.mule.services.soap.impl.generator.attachment.AttachmentResponseEnricher;
import org.mule.services.soap.impl.message.ImmutableSoapResponse;
import org.mule.services.soap.impl.util.XmlTransformationUtils;
import org.mule.services.soap.impl.util.XmlTransformationException;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.message.Exchange;
import org.w3c.dom.Document;

/**
 * Class used to generate the output of the {@link ConsumeOperation} using the CXF response.
 *
 * @since 4.0
 */
public final class SoapResponseGenerator {

  private final AttachmentResponseEnricher responseEnricher;

  public SoapResponseGenerator(AttachmentResponseEnricher responseEnricher) {
    this.responseEnricher = responseEnricher;
  }

  /**
   * Generates an {@link Result} with the out attachments and headers and the response body of the SOAP operation.
   * <p>
   * If there are out attachments the nodes in the response associated to them will be removed so the end user don't need to
   * handle those nodes.
   * <p>
   * The our SOAP headers and the protocol specific headers will be retrieved in a {@link SoapAttributes} as attributes in the
   * returned {@link Result}.
   *
   * @param operation the name of the operation that was invoked
   * @param response  the CXF response
   * @param exchange  the exchange used for CXF to store the headers and attachments.
   */
  public SoapResponse generate(String operation, Object[] response, Exchange exchange) {
    Document document = unwrapResponse(response);
    // Streaming
    String result = responseEnricher.enrich(document, operation, exchange);
    List<SoapAttachment> attachments = (List<SoapAttachment>) exchange.get(SoapCxfClient.MULE_ATTACHMENTS_KEY);
    Map<String, String> soapHeaders = (Map<String, String>) exchange.get(SoapCxfClient.MULE_HEADERS_KEY);
    ByteArrayInputStream resultStream = new ByteArrayInputStream(result.getBytes());
    return new ImmutableSoapResponse(resultStream, soapHeaders, emptyMap(), attachments, APPLICATION_XML);
  }

  /**
   * Unwraps the CXF {@link XMLStreamReader} response into a dom {@link Document}.
   *
   * @param response the CXF received response.
   */
  private Document unwrapResponse(Object[] response) {
    if (response.length == 0) {
      throw new BadResponseException("no elements were received in the SOAP response.");
    }
    if (response.length != 1) {
      throw new BadResponseException("the obtained response contains more than one element, only one was expected");
    }
    XMLStreamReader reader = (XMLStreamReader) response[0];
    try {
      return XmlTransformationUtils.xmlStreamReaderToDocument(reader);
    } catch (XmlTransformationException e) {
      throw new BadResponseException("Error transforming the XML web service response to be processed", e);
    }
  }
}
