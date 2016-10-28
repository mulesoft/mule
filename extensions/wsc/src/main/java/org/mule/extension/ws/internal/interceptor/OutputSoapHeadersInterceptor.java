/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.interceptor;

import static java.util.stream.Collectors.toMap;
import static org.apache.cxf.phase.Phase.PRE_PROTOCOL;
import static org.mule.extension.ws.internal.ConsumeOperation.MULE_HEADERS_KEY;
import org.mule.extension.ws.api.WscAttributes;
import org.mule.extension.ws.internal.ConsumeOperation;

import java.io.StringWriter;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.w3c.dom.Node;

/**
 * CXF out interceptor that collects the received SOAP headers in the response, transforms it and stores them in the response
 * message {@link Exchange} so then can be returned by the {@link ConsumeOperation} in the {@link WscAttributes}.
 *
 * @since 4.0
 */
public class OutputSoapHeadersInterceptor extends AbstractSoapInterceptor {

  public OutputSoapHeadersInterceptor() {
    super(PRE_PROTOCOL);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleMessage(SoapMessage message) throws Fault {
    Map<String, String> result = message.getHeaders().stream()
        .filter(header -> header instanceof SoapHeader)
        .collect(toMap(h -> h.getName().getLocalPart(), this::nodeToString));

    message.getExchange().put(MULE_HEADERS_KEY, result);
  }

  private String nodeToString(Header header) {
    // TODO: review this transformation.
    try {
      StringWriter writer = new StringWriter();
      Node node = (Node) header.getObject();
      DOMSource source = new DOMSource(node);
      StreamResult result = new StreamResult(writer);
      TransformerFactory idTransformer = new SaxonTransformerFactory();
      Transformer transformer = idTransformer.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.transform(source, result);
      return writer.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
