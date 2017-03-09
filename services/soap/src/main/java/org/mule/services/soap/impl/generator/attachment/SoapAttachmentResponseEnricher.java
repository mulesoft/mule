/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.generator.attachment;

import static java.lang.String.format;
import static org.mule.services.soap.impl.client.SoapCxfClient.MULE_ATTACHMENTS_KEY;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.codec.Base64Decoder;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.services.soap.api.message.ImmutableSoapAttachment;
import org.mule.services.soap.impl.exception.EncodingException;
import org.mule.services.soap.impl.introspection.WsdlIntrospecter;

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
    List<ImmutableSoapAttachment> result = attachments.stream().map(a -> {
      String tagName = a.getKey().getName().getLocalPart();
      Node attachmentNode = response.getDocumentElement().getElementsByTagName(tagName).item(0);
      String decodedAttachment = decodeAttachment(tagName, attachmentNode.getTextContent());
      response.getDocumentElement().removeChild(attachmentNode);
      return new ImmutableSoapAttachment(tagName, MediaType.ANY, new ByteArrayInputStream(decodedAttachment.getBytes()));
    }).collect(new ImmutableListCollector<>());
    exchange.put(MULE_ATTACHMENTS_KEY, result);
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
