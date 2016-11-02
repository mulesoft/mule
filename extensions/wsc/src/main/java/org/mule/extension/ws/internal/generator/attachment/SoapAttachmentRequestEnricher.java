/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator.attachment;

import org.mule.extension.ws.api.SoapAttachment;
import org.mule.extension.ws.api.exception.WscEncodingException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.codec.Base64Encoder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * {@link AttachmentRequestEnricher} implementation to send attachments using SOAP with Attachments.
 *
 * @since 4.0
 */
public final class SoapAttachmentRequestEnricher extends AttachmentRequestEnricher {

  private static final Base64Encoder encoder = new Base64Encoder();

  /**
   * {@inheritDoc}
   * <p>
   * Adds the attachment content encoded to Base64 plain in the XML Request in the generated attachment node.
   */
  @Override
  protected void addAttachmentElement(Document bodyDocument, SoapAttachment soapAttachment, Element attachmentElement) {
    // Encode the attachment to base64 to be sent as SOAP with Attachments.
    attachmentElement.setTextContent(toBase64(soapAttachment.getContent()));
  }

  /**
   * Returns an {@link String} with the content of the attachment encoded to Base64.
   *
   * @param content the content of the attachment.
   */
  private String toBase64(Object content) {
    try {
      return encoder.transform(content).toString();
    } catch (TransformerException e) {
      throw new WscEncodingException("Could not encode attachment content to base64", e);
    }
  }
}
