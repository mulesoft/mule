/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;


import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import org.mule.extension.ws.api.WsAttachment;
import org.mule.extension.ws.api.WscAttributes;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.introspection.RequestBodyGenerator;
import org.mule.extension.ws.internal.metadata.OperationKeysResolver;
import org.mule.extension.ws.internal.metadata.WscAttributesResolver;
import org.mule.extension.ws.internal.metadata.body.InputBodyResolver;
import org.mule.extension.ws.internal.metadata.body.OutputBodyResolver;
import org.mule.extension.ws.internal.metadata.header.InputHeadersResolver;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.xml.stax.StaxSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.jaxp.SaxonTransformerFactory;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * The only {@link WebServiceConsumer} operation which consumes an operation of the connected web service and returns it's
 * response.
 * <p>
 * The consume operation expects an XML body and a set of headers and attachments if required.
 * <p>
 * For the cases where no input parameters are required the {@link RequestBodyGenerator} will generate a body to perform the
 * operation if a {@code null} value is passed.
 *
 * @since 4.0
 */
public class ConsumeOperation {

  public static final String MULE_ATTACHMENTS_KEY = "mule.wsc.attachments";
  public static final String MULE_HEADERS_KEY = "mule.wsc.headers";
  public static final String MULE_SOAP_ACTION = "mule.wsc.soap.action";

  @OutputResolver(output = OutputBodyResolver.class, attributes = WscAttributesResolver.class)
  public OperationResult<String, WscAttributes> consume(@Connection WscConnection connection,
                                                        @MetadataKeyId(OperationKeysResolver.class) String operation,
                                                        @Optional @Content @TypeResolver(InputBodyResolver.class) String body,
                                                        @Optional @TypeResolver(InputHeadersResolver.class) Map<String, String> headers,
                                                        @Optional List<WsAttachment> attachments)
      throws Exception {
    Map<String, Object> ctx = getContext(headers, attachments, operation);
    Exchange exchange = new ExchangeImpl();
    XMLStreamReader request = stringToXmlStreamReader(body, connection, operation);
    Object[] response = connection.invoke(request, ctx, exchange);
    return asResult(processOutput(response), processAttributes(exchange));
  }

  private WscAttributes processAttributes(Exchange exchange) {
    Map<String, String> headers = (Map<String, String>) exchange.get(MULE_HEADERS_KEY);
    return new WscAttributes(headers, emptyMap());
  }

  private Map<String, Object> getContext(Map<String, String> headers, List<WsAttachment> attachments, String operation) {
    Map<String, Object> props = new HashMap<>();
    props.put(MULE_ATTACHMENTS_KEY, transformAttachments(attachments));
    props.put(MULE_HEADERS_KEY, transformHeaders(headers));
    props.put(MULE_SOAP_ACTION, operation);

    Map<String, Object> ctx = new HashMap<>();
    ctx.put(Client.REQUEST_CONTEXT, props);
    return ctx;
  }

  private OperationResult<String, WscAttributes> asResult(String output, WscAttributes attributes) {
    return OperationResult.<String, WscAttributes>builder().output(output).attributes(attributes).build();
  }

  private String processOutput(Object[] response) throws Exception {
    XMLStreamReader xmlStreamReader = (XMLStreamReader) response[0];
    StaxSource staxSource = new StaxSource(xmlStreamReader);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory idTransformer = new SaxonTransformerFactory();
    Transformer transformer = idTransformer.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.transform(staxSource, result);
    return writer.getBuffer().toString();
  }

  private List<SoapHeader> transformHeaders(Map<String, String> headers) {
    if (headers == null) {
      return emptyList();
    }
    return headers.entrySet().stream()
        .map(h -> new SoapHeader(new QName(null, h.getKey()), stringToDocument(h.getValue())))
        .collect(new ImmutableListCollector<>());
  }

  private List<Attachment> transformAttachments(List<WsAttachment> attachments) {
    if (attachments == null) {
      return emptyList();
    }
    return attachments.stream().map(a -> {
      try {
        return new AttachmentImpl(a.getId(), toDataHandler(a.getId(), a.getContent(), a.getContentType()));
      } catch (IOException e) {
        throw new WscException("Error while preparing attachments", e);
      }
    }).collect(new ImmutableListCollector<>());
  }

  private Element stringToDocument(String xml) {
    try {
      DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      InputSource is = new InputSource();
      is.setCharacterStream(new StringReader(xml));
      return db.parse(is).getDocumentElement();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private XMLStreamReader stringToXmlStreamReader(String body, WscConnection connection, String operation) {
    try {
      if (isBlank(body)) {
        body = new RequestBodyGenerator().generateRequest(connection, operation);
      }
      return XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(body.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
