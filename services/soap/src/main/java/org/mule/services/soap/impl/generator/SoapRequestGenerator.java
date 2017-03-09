/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.impl.generator;

import static org.apache.commons.lang.StringUtils.isBlank;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.services.soap.api.message.SoapAttachment;
import org.mule.services.soap.impl.exception.SoapServiceException;
import org.mule.services.soap.impl.generator.attachment.AttachmentRequestEnricher;
import org.mule.services.soap.impl.introspection.WsdlIntrospecter;
import org.mule.services.soap.impl.util.XmlTransformationUtils;
import org.mule.services.soap.impl.util.XmlTransformationException;

import java.util.List;

import javax.xml.stream.XMLStreamReader;

/**
 * Generates a XML SOAP request used to invoke CXF.
 * <p>
 * If no body is provided will try to generate a default one.
 * <p>
 * for each attachment will add a node with the required information depending on the protocol that it's being used.
 *
 * @since 4.0
 */
public final class SoapRequestGenerator {

  private final EmptyRequestGenerator emptyRequestGenerator;
  private final AttachmentRequestEnricher requestEnricher;

  public SoapRequestGenerator(AttachmentRequestEnricher requestEnricher, WsdlIntrospecter introspecter, XmlTypeLoader loader) {
    this.requestEnricher = requestEnricher;
    this.emptyRequestGenerator = new EmptyRequestGenerator(introspecter, loader);
  }

  /**
   * Generates an {@link XMLStreamReader} SOAP request ready to be consumed by CXF.
   * @param operation   the name of the operation being invoked.
   * @param body        the body content provided by the user.
   * @param attachments the attachments provided by the user.
   */
  public XMLStreamReader generate(String operation, String body, List<SoapAttachment> attachments) {

    if (isBlank(body)) {
      body = emptyRequestGenerator.generateRequest(operation);
    }

    if (!attachments.isEmpty()) {
      body = requestEnricher.enrichRequest(body, attachments);
    }

    try {
      return XmlTransformationUtils.stringToXmlStreamReader(body);
    } catch (XmlTransformationException e) {
      throw new SoapServiceException("Error generating SOAP request", e);
    }
  }
}
