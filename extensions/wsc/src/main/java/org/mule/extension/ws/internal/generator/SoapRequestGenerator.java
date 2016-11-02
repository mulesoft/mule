/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.extension.ws.internal.util.TransformationUtils.stringToXmlStreamReader;
import org.mule.extension.ws.api.SoapAttachment;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;
import org.mule.extension.ws.internal.util.WscTransformationException;
import org.mule.metadata.api.TypeLoader;

import java.util.Map;

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

  private static final EmptyRequestGenerator emptyRequestGenerator = new EmptyRequestGenerator();

  /**
   * Generates an {@link XMLStreamReader} SOAP request ready to be consumed by CXF.
   *
   * @param connection  the connection used to send the request.
   * @param operation   the name of the operation being invoked.
   * @param body        the body content provided by the user.
   * @param attachments the attachments provided by the user.
   */
  public XMLStreamReader generate(WscConnection connection, String operation, String body,
                                  Map<String, SoapAttachment> attachments) {

    WsdlIntrospecter introspecter = connection.getWsdlIntrospecter();
    TypeLoader typeLoader = connection.getTypeLoader();

    if (isBlank(body)) {
      body = emptyRequestGenerator.generateRequest(introspecter, typeLoader, operation);
    }

    if (!attachments.isEmpty()) {
      body = connection.getRequestEnricher().enrichRequest(body, attachments);
    }

    try {
      return stringToXmlStreamReader(body);
    } catch (WscTransformationException e) {
      throw new WscException("Error generating SOAP request", e);
    }
  }
}
