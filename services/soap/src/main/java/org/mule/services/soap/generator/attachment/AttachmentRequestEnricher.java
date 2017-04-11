/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.generator.attachment;

import static org.mule.services.soap.impl.xml.util.XMLUtils.toXml;
import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.services.soap.api.exception.SoapServiceException;
import org.mule.services.soap.introspection.WsdlIntrospecter;
import org.mule.services.soap.util.XmlTransformationException;
import org.mule.services.soap.util.XmlTransformationUtils;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract implementation for a request enricher that adds a node for each sent attachment to the incoming SOAP
 * request with all the information required to send the attachments using the SOAP protocol.
 *
 * @since 4.0
 */
public abstract class AttachmentRequestEnricher {

  protected WsdlIntrospecter introspecter;
  protected TypeLoader loader;

  AttachmentRequestEnricher(WsdlIntrospecter introspecter, TypeLoader loader) {
    this.introspecter = introspecter;
    this.loader = loader;
  }

  /**
   * @param body        the XML SOAP body provided by the user.
   * @param attachments the attachments to upload.
   */
  public String enrichRequest(String body, Map<String, SoapAttachment> attachments) {
    try {
      Document bodyDocument = XmlTransformationUtils.stringToDocument(body);
      Element documentElement = bodyDocument.getDocumentElement();
      attachments.forEach((name, attachment) -> {
        Element attachmentElement = bodyDocument.createElement(name);
        addAttachmentElement(bodyDocument, name, attachment, attachmentElement);
        documentElement.appendChild(attachmentElement);
      });
      return toXml(bodyDocument);
    } catch (XmlTransformationException e) {
      throw new SoapServiceException("Error while preparing request for the provided body", e);
    }
  }

  /**
   * Adds the content to the attachment node recently created to the XML SOAP request
   *
   * @param bodyDocument      the document where we are adding the node element.
   * @param attachment      the attachment to be sent.
   * @param attachmentElement the recently created attachment node in the xml request.
   */
  abstract void addAttachmentElement(Document bodyDocument, String name, SoapAttachment attachment, Element attachmentElement);

}
