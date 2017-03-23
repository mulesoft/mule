/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.generator.attachment;

import static java.lang.String.format;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.services.soap.impl.exception.InvalidWsdlException;
import org.mule.services.soap.impl.metadata.OutputTypeIntrospecterDelegate;
import org.mule.services.soap.impl.introspection.WsdlIntrospecter;
import org.mule.services.soap.impl.util.SoapServiceMetadataTypeUtils;
import org.mule.services.soap.impl.xml.util.XMLUtils;

import java.util.List;

import javax.wsdl.Part;

import org.apache.cxf.message.Exchange;
import org.w3c.dom.Document;

/**
 *
 * @since 4.0
 */
public abstract class AttachmentResponseEnricher {

  private final WsdlIntrospecter introspecter;
  private final TypeLoader loader;

  protected AttachmentResponseEnricher(WsdlIntrospecter introspecter, TypeLoader loader) {
    this.introspecter = introspecter;
    this.loader = loader;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Modifies the SOAP response to avoid attachment content in the response body and make decouple the attachment handling so
   * the user can have a better experience.
   */
  public String enrich(Document response, String operation, Exchange exchange) {
    Part outputPart = introspecter.getBodyPart(operation, new OutputTypeIntrospecterDelegate())
        .orElseThrow(() -> new InvalidWsdlException(
                                                    format("Cannot find output body part for operation [%s] in the configured WSDL",
                                                           operation)));

    String part = outputPart.getElementName().toString();
    MetadataType outputBodyType = loader.load(part)
        .orElseThrow(() -> new InvalidWsdlException(
                                                    format("Cannot found output part [%s] for operation [%s] in the configured WSDL",
                                                           part, operation)));

    List<ObjectFieldType> attachmentParams = SoapServiceMetadataTypeUtils.getAttachmentFields(outputBodyType);
    if (!attachmentParams.isEmpty()) {
      processResponseAttachments(response, attachmentParams, exchange);
    }
    return XMLUtils.toXml(response);
  }

  /**
   * Processes the attachments nodes in the response.
   */
  protected abstract void processResponseAttachments(Document response, List<ObjectFieldType> attachments, Exchange exchange);
}
