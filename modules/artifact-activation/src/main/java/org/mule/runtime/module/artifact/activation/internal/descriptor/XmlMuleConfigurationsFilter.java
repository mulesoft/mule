/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import static java.lang.String.format;

import org.mule.runtime.module.artifact.activation.api.descriptor.MuleConfigurationsFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class XmlMuleConfigurationsFilter implements MuleConfigurationsFilter {

  private static final String DEFAULT_NAMESPACE_URI_MASK = "http://www.mulesoft.org/schema/mule/%s";
  private static final String CORE_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "core");
  private static final String DOMAIN_NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, "domain");
  private static final String CONFIG_FILE_EXTENSION = ".xml";
  private static final String muleLocalName = "mule";
  private static final String muleDomainLocalName = "mule-domain";

  private final XMLInputFactory factory = XMLInputFactory.newInstance();

  @Override
  public boolean filter(File candidateConfig) {
    return candidateConfig.getName().endsWith(CONFIG_FILE_EXTENSION)
        && hasMuleAsRootElement(generateDocument(candidateConfig.toPath()));
  }

  private boolean hasMuleAsRootElement(QName docRootTagName) {
    if (docRootTagName != null) {
      String rootElementLocalName = docRootTagName.getLocalPart();
      String rootElementNamespace = docRootTagName.getNamespaceURI();

      return (rootElementLocalName.equals(muleLocalName) && rootElementNamespace.equals(CORE_NAMESPACE))
          || (rootElementLocalName.equals(muleDomainLocalName) && rootElementNamespace.equals(DOMAIN_NAMESPACE));
    }
    return false;
  }

  private QName generateDocument(Path filePath) {
    XMLEventReader eventReader = null;
    try {
      eventReader =
          factory.createXMLEventReader(new FileReader(filePath.toFile()));

      while (eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();

        if (event.isStartElement()) {
          // early return
          return ((StartElement) event).getName();
        }
      }
      return null;
    } catch (FileNotFoundException | XMLStreamException e) {
      return null;
    } finally {
      if (eventReader == null) {
        return null;
      }
      try {
        eventReader.close();
      } catch (XMLStreamException e) {
        return null;
      }
    }
  }
}
