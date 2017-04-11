/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.generator.attachment;

import static java.lang.String.format;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.codec.Base64Decoder;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.services.soap.api.exception.EncodingException;
import org.mule.services.soap.client.SoapCxfClient;
import org.mule.services.soap.introspection.WsdlIntrospecter;

import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.cxf.message.Exchange;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * {@link AttachmentResponseEnricher} implementation for SOAP with attachments.
 *
 * @since 4.0
 */
public final class SoapAttachmentResponseEnricher extends AttachmentResponseEnricher {

  private static final Base64Decoder decoder = new Base64Decoder();

  public SoapAttachmentResponseEnricher(WsdlIntrospecter introspecter, TypeLoader loader) {
    super(introspecter, loader);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Extracts the base64 encoded content from the attachment nodes, decodes them and then remove all the nodes to
   * clean the response body.
   */
  @Override
  protected void processResponseAttachments(Document response, List<ObjectFieldType> attachments, Exchange exchange) {
    ImmutableMap.Builder<String, SoapAttachment> builder = ImmutableMap.builder();
    attachments.forEach(attachment -> {
      String attachmentName = getLocalPart(attachment);
      builder.put(attachmentName, getAttachment(response, attachmentName));
    });
    exchange.put(SoapCxfClient.MULE_ATTACHMENTS_KEY, builder.build());
  }

  /**
   * Extracts the base64 encoded content from the attachment {@code name}, decodes its content and removes the node from the
   * document.
   */
  private SoapAttachment getAttachment(Document response, String name) {
    Node attachmentNode = response.getDocumentElement().getElementsByTagName(name).item(0);
    String decodedAttachment = decodeAttachment(name, attachmentNode.getTextContent());
    response.getDocumentElement().removeChild(attachmentNode);
    return new SoapAttachment(new ByteArrayInputStream(decodedAttachment.getBytes()), ANY);
  }

  /**
   * Decodes the attachment content from base64.
   */
  private String decodeAttachment(String name, String attachmentContent) {
    try {
      return new String((byte[]) decoder.transform(attachmentContent));
    } catch (TransformerException e) {
      throw new EncodingException(format("Cannot decode base64 attachment [%s]", name));
    }
  }
}
