/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;


import static java.lang.String.format;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import org.mule.extension.ws.api.SoapAttachment;
import org.mule.extension.ws.api.SoapMessageBuilder;
import org.mule.extension.ws.api.WscAttributes;
import org.mule.extension.ws.api.exception.BadRequestException;
import org.mule.extension.ws.api.exception.SoapFaultException;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.generator.SoapRequestGenerator;
import org.mule.extension.ws.internal.generator.SoapResponseGenerator;
import org.mule.extension.ws.internal.metadata.ConsumeOutputResolver;
import org.mule.extension.ws.internal.metadata.MessageBuilderResolver;
import org.mule.extension.ws.internal.metadata.OperationKeysResolver;
import org.mule.extension.ws.internal.metadata.WscAttributesResolver;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.xml.util.XMLUtils;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The only {@link WebServiceConsumer} operation. the {@link ConsumeOperation} consumes an operation of the connected web service
 * and returns it's response.
 * <p>
 * The consume operation expects an XML body and a set of headers and attachments if required.
 * <p>
 * For the cases where no input parameters are required the {@link SoapRequestGenerator} will generate a body to perform the
 * operation if a {@code null} value is passed.
 *
 * @since 4.0
 */
public class ConsumeOperation {

  private final SoapRequestGenerator requestGenerator = new SoapRequestGenerator();
  private final SoapResponseGenerator responseGenerator = new SoapResponseGenerator();

  /**
   * Consumes an operation from a SOAP Web Service.
   *
   * @param connection the connection resolved to execute the operation.
   * @param operation  the name of the web service operation that aims to invoke.
   * @param message    the constructed SOAP message to perform the request.
   */
  @OnException(WscExceptionEnricher.class)
  @Throws(ConsumeErrorTypeProvider.class)
  @OutputResolver(output = ConsumeOutputResolver.class, attributes = WscAttributesResolver.class)
  public Result<Object, WscAttributes> consume(@UseConfig WebServiceConsumer config,
                                               @Connection WscConnection connection,
                                               @MetadataKeyId(OperationKeysResolver.class) String operation,
                                               //TODO MULE-11235
                                               @NullSafe @Optional @TypeResolver(MessageBuilderResolver.class) SoapMessageBuilder message)
      throws SoapFaultException {
    Map<String, SoapAttachment> attachments = message.getAttachments();
    XMLStreamReader request = requestGenerator.generate(connection, operation, message.getBody(), attachments);
    Exchange exchange = new ExchangeImpl();
    Object[] response = connection.invoke(operation,
                                          request,
                                          transformToCxfHeaders(message.getHeaders()),
                                          transformToCxfAttachments(attachments),
                                          config.getEncoding(),
                                          exchange);
    return responseGenerator.generate(connection, operation, response, exchange);
  }

  /**
   * Prepares the provided {@link Map} of headers in the {@link SoapMessageBuilder} to be processed by CXF.
   *
   * @param headers
   */
  private List<SoapHeader> transformToCxfHeaders(String headers) {
    if (headers == null) {
      return Collections.emptyList();
    }
    ImmutableList.Builder<SoapHeader> soapHeaders = ImmutableList.builder();
    try {
      Element document = XMLUtils.toW3cDocument(headers).getDocumentElement();
      NodeList nodes = document.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
        Node currentNode = nodes.item(i);
        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
          soapHeaders.add(new SoapHeader(new QName(null, currentNode.getNodeName()), currentNode));
        }
      }
    } catch (Exception e) {
      throw new BadRequestException("Error while parsing the provided soap headers", e);
    }
    return soapHeaders.build();
  }

  /**
   * Prepares the provided {@link Map} of attachments in the {@link SoapMessageBuilder} to be processed by CXF.
   */
  private List<Attachment> transformToCxfAttachments(Map<String, SoapAttachment> attachments) {
    return attachments.values().stream().map(a -> {
      try {
        return new AttachmentImpl(a.getId(), toDataHandler(a.getId(), a.getContent(), a.getContentType()));
      } catch (IOException e) {
        throw new BadRequestException(format("Error while preparing attachment [%s] for upload", a.getId()), e);
      }
    }).collect(new ImmutableListCollector<>());
  }
}
