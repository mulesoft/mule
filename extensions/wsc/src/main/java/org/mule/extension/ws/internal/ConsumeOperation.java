/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;


import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.mule.extension.ws.internal.util.TransformationUtils.stringToDomElement;
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
import org.mule.extension.ws.internal.util.WscTransformationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;

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

  public static final String MULE_ATTACHMENTS_KEY = "mule.wsc.attachments";
  public static final String MULE_HEADERS_KEY = "mule.wsc.headers";
  public static final String MULE_SOAP_ACTION = "mule.wsc.soap.action";
  public static final String MULE_WSC_ENCODING = "mule.wsc.encoding";

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
                                               @TypeResolver(MessageBuilderResolver.class) SoapMessageBuilder message)
      throws SoapFaultException {
    Map<String, SoapAttachment> attachments = message.getAttachments();
    Map<String, String> headers = message.getHeaders();
    Map<String, Object> ctx = getInvocationContext(config, connection, headers, attachments, operation);
    XMLStreamReader request = requestGenerator.generate(connection, operation, message.getBody(), attachments);
    Exchange exchange = new ExchangeImpl();
    Object[] response = connection.invoke(operation, request, ctx, exchange);
    return responseGenerator.generate(connection, operation, response, exchange);
  }

  /**
   * Sets up a request context where the attachments, headers and the soap action required by cxf are populated.
   */
  private Map<String, Object> getInvocationContext(WebServiceConsumer config,
                                                   WscConnection connection,
                                                   Map<String, String> headers,
                                                   Map<String, SoapAttachment> attachments,
                                                   String operation) {
    Map<String, Object> props = new HashMap<>();

    // If is NOT MTOM the attachments must not be touched by cxf, we create a custom request embedding the attachment in the xml.
    if (connection.isMtomEnabled()) {
      props.put(MULE_ATTACHMENTS_KEY, transformToCxfAttachments(attachments));
    } else {
      props.put(MULE_ATTACHMENTS_KEY, emptyList());
    }

    props.put(MULE_WSC_ENCODING, config.getEncoding());
    props.put(MULE_HEADERS_KEY, transformToCxfHeaders(headers));
    props.put(MULE_SOAP_ACTION, operation);
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(Client.REQUEST_CONTEXT, props);
    return ctx;
  }

  /**
   * Prepares the provided {@link Map} of headers in the {@link SoapMessageBuilder} to be processed by CXF.
   */
  private List<SoapHeader> transformToCxfHeaders(Map<String, String> headers) {
    return headers.entrySet().stream()
        .map(h -> {
          try {
            return new SoapHeader(new QName(null, h.getKey()), stringToDomElement(h.getValue()));
          } catch (WscTransformationException e) {
            throw new BadRequestException(format("Error while preparing request header [%s] to be sent", h.getKey()), e);
          }
        })
        .collect(new ImmutableListCollector<>());
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
