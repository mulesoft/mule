/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import static java.util.Collections.emptyMap;
import static org.mule.extension.ws.internal.ConsumeOperation.MULE_ATTACHMENTS_KEY;
import static org.mule.extension.ws.internal.ConsumeOperation.MULE_HEADERS_KEY;
import static org.mule.extension.ws.internal.util.TransformationUtils.xmlStreamReaderToDocument;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import org.mule.extension.ws.api.WscAttributes;
import org.mule.extension.ws.api.WscMultipartPayload;
import org.mule.extension.ws.api.exception.BadResponseException;
import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.extension.ws.internal.util.WscTransformationException;
import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.operation.Result;

import com.google.common.collect.ImmutableList;

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

  /**
   * Generates an {@link Result} with the out attachments and headers and the response body of the SOAP operation.
   * <p>
   * If there are out attachments the nodes in the response associated to them will be removed so the end user don't need to
   * handle those nodes.
   * <p>
   * The our SOAP headers and the protocol specific headers will be retrieved in a {@link WscAttributes} as attributes in the
   * returned {@link Result}.
   *
   * @param connection the connection used to invoke the operation
   * @param operation  the name of the operation that was invoked
   * @param response   the CXF response
   * @param exchange   the exchange used for CXF to store the headers and attachments.
   */
  public Result<Object, WscAttributes> generate(WscConnection connection,
                                                String operation,
                                                Object[] response,
                                                Exchange exchange) {
    Document document = unwrapResponse(response);
    WsdlIntrospecter introspecter = connection.getWsdlIntrospecter();
    TypeLoader loader = connection.getTypeLoader();

    String result = connection.getResponseEnricher().enrich(document, introspecter, loader, operation, exchange);

    WscAttributes attributes = processAttributes(exchange);
    List<Message> receivedAttachments = (List<Message>) exchange.get(MULE_ATTACHMENTS_KEY);

    Object output;
    if (!receivedAttachments.isEmpty()) {
      ImmutableList<Message> parts = ImmutableList.<Message>builder()
          .add(Message.builder().payload(result).attributes(BODY_ATTRIBUTES).build())
          .addAll(receivedAttachments)
          .build();
      output = new WscMultipartPayload(parts);
    } else {
      output = result;
    }

    return Result.<Object, WscAttributes>builder()
        .output(output)
        .attributes(attributes)
        .build();
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
      return xmlStreamReaderToDocument(reader);
    } catch (WscTransformationException e) {
      throw new BadResponseException("Error transforming the XML web service response to be processed", e);
    }
  }

  private WscAttributes processAttributes(Exchange exchange) {
    Map<String, String> headers = (Map<String, String>) exchange.get(MULE_HEADERS_KEY);
    return new WscAttributes(headers, emptyMap());
  }
}
