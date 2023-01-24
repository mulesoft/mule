/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createDefault;

import org.mule.runtime.module.artifact.activation.api.descriptor.MuleConfigurationsFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlMuleConfigurationsFilter implements MuleConfigurationsFilter {

  private static final String CONFIG_FILE_EXTENSION = ".xml";
  private static final String muleLocalName = "mule";
  private static final String muleDomainLocalName = "domain:mule-domain";
  private static final String muleNamespace = "http://www.mulesoft.org/schema/mule/core";
  private static final String muleDomainNamespace = "http://www.mulesoft.org/schema/mule/domain";

  @Override
  public boolean filter(File candidateConfig) {
    return candidateConfig.getName().endsWith(CONFIG_FILE_EXTENSION)
        && hasMuleAsRootElement(generateDocument(candidateConfig.toPath()));
  }

  private boolean hasMuleAsRootElement(Document doc) {
    if (doc != null && doc.getDocumentElement() != null) {
      String rootElementLocalName = doc.getDocumentElement().getLocalName();
      String rootElementNamespace = doc.getDocumentElement().getNamespaceURI();

      return (rootElementLocalName.equals(muleLocalName) && rootElementNamespace.equals(muleNamespace))
          || (rootElementLocalName.equals(muleDomainLocalName) && rootElementNamespace.equals(muleDomainNamespace));
    }
    return false;
  }

  private Document generateDocument(Path filePath) {
    DocumentBuilderFactory documentBuilderFactory = createDefault().getDocumentBuilderFactory();
    documentBuilderFactory.setNamespaceAware(true);

    try {
      return documentBuilderFactory.newDocumentBuilder().parse(filePath.toFile());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      return null;
    }
  }
}
