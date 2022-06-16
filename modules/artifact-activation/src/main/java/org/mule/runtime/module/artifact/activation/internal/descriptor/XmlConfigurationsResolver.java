/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static org.mule.runtime.internal.dsl.DslConstants.EE_NAMESPACE;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class XmlConfigurationsResolver implements ConfigurationsResolver {

  private static final String CONFIG_FILE_EXTENSION = ".xml";

  @Override
  public List<DeployableConfiguration> resolve(List<File> candidateConfigs) {
    return candidateConfigs.stream().filter(candidateConfig -> candidateConfig.getName().endsWith(CONFIG_FILE_EXTENSION))
        .map(candidateConfig -> new Pair<>(candidateConfig, generateDocument(candidateConfig.toPath())))
        .filter(pair -> hasMuleAsRootElement(pair.getSecond()))
        .map(pair -> new DeployableConfiguration(pair.getFirst().getName(),
                                                 containsEENamespace(pair.getSecond()) ? Product.MULE_EE : Product.MULE))
        .collect(toList());
  }

  private boolean hasMuleAsRootElement(Document doc) {
    if (doc != null && doc.getDocumentElement() != null) {
      String rootElementName = doc.getDocumentElement().getTagName();
      return StringUtils.equals(rootElementName, "mule") || StringUtils.equals(rootElementName, "domain:mule-domain");
    }
    return false;
  }

  private Document generateDocument(Path filePath) {
    javax.xml.parsers.DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();

    try {
      return factory.newDocumentBuilder().parse(filePath.toFile());
    } catch (IOException | ParserConfigurationException | SAXException e) {
      return null;
    }
  }

  private boolean containsEENamespace(Document doc) {
    if (doc == null) {
      return false;
    }
    org.w3c.dom.Element root = doc.getDocumentElement();
    if (root.getNamespaceURI() != null && root.getNamespaceURI().contains(EE_NAMESPACE)) {
      return true;
    }
    if (root.getAttributes() != null) {
      NamedNodeMap attributes = root.getAttributes();
      for (int i = 0; i < attributes.getLength(); ++i) {
        Node uri = root.getAttributes().item(i);
        if (uri.getNodeValue() != null && uri.getNodeValue().contains(EE_NAMESPACE)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Creates a document builder factory.
   *
   * @return the factory created
   */
  private DocumentBuilderFactory createSecureDocumentBuilderFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      // Configuration based on
      // https://github.com/OWASP/CheatSheetSeries/blob/master/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.md#xpathexpression

      // This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all
      // XML entity attacks are prevented
      // Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
      String feature = "http://apache.org/xml/features/disallow-doctype-decl";
      factory.setFeature(feature, true);

      // If you can't completely disable DTDs, then at least do the following:
      // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
      // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
      // JDK7+ - http://xml.org/sax/features/external-general-entities
      feature = "http://xml.org/sax/features/external-general-entities";
      factory.setFeature(feature, false);

      // Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
      // Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
      // JDK7+ - http://xml.org/sax/features/external-parameter-entities
      feature = "http://xml.org/sax/features/external-parameter-entities";
      factory.setFeature(feature, false);

      // Disable external DTDs as well
      feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
      factory.setFeature(feature, false);

      // and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      return factory;
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);// should never happen
    }
  }

}
