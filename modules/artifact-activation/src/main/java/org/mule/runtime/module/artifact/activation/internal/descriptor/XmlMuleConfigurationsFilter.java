/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createDefault;

import static java.lang.String.format;

import org.mule.runtime.module.artifact.activation.api.descriptor.MuleConfigurationsFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlMuleConfigurationsFilter implements MuleConfigurationsFilter {

  private static final String DEFAULT_NAMESPACE_URI_MASK = "http://www.mulesoft.org/schema/mule/%s";
  private static final String CORE_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "core");
  private static final String DOMAIN_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "domain");
  private static final String CONFIG_FILE_EXTENSION = ".xml";
  private static final String muleLocalName = "mule";
  private static final String muleDomainLocalName = "mule-domain";

  @Override
  public boolean filter(File candidateConfig) {
    return candidateConfig.getName().endsWith(CONFIG_FILE_EXTENSION)
        && hasMuleAsRootElement(generateDocument(candidateConfig.toPath()));
  }

  private boolean hasMuleAsRootElement(Document doc) {
    if (doc != null && doc.getDocumentElement() != null) {
      String rootElementLocalName = doc.getDocumentElement().getLocalName();
      String rootElementNamespace = doc.getDocumentElement().getNamespaceURI();

      return (rootElementLocalName.equals(muleLocalName) && rootElementNamespace.equals(CORE_NAMESPACE))
          || (rootElementLocalName.equals(muleDomainLocalName) && rootElementNamespace.equals(DOMAIN_NAMESPACE));
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
