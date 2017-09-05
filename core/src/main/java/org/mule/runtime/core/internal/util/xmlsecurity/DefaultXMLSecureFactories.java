/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xmlsecurity;

import static java.lang.String.format;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create different XML factories configured through the same interface for disabling vulnerabilities.
 *
 * Also make sure we are using standard Java implementations when not overriding explicitly. This is necessary
 * as some dependencies such as Woodstox and Saxon register service providers that take precedence over
 * the Java defaults (in META-INF/services).
 */
public class DefaultXMLSecureFactories {

  private Boolean externalEntities;
  private Boolean expandEntities;

  private final static Log logger = LogFactory.getLog(DefaultXMLSecureFactories.class);

  public DefaultXMLSecureFactories(Boolean externalEntities, Boolean expandEntities) {
    this.externalEntities = externalEntities;
    this.expandEntities = expandEntities;
  }

  public DocumentBuilderFactory createDocumentBuilderFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    try {
      factory.setFeature("http://xml.org/sax/features/external-general-entities", externalEntities);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", externalEntities);
      factory.setExpandEntityReferences(expandEntities);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !expandEntities);
    } catch (Exception e) {
      logConfigurationWarning("DocumentBuilderFactory", factory.getClass().getName(), e);
    }

    return factory;
  }

  public SAXParserFactory createSaxParserFactory() {
    SAXParserFactory factory = SAXParserFactory.newInstance();

    try {
      factory.setFeature("http://xml.org/sax/features/external-general-entities", externalEntities);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", externalEntities);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !expandEntities);
    } catch (Exception e) {
      logConfigurationWarning("SAXParserFactory", factory.getClass().getName(), e);
    }

    return factory;
  }

  public XMLInputFactory createXMLInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance();

    configureXMLInputFactory(factory);

    return factory;
  }

  public TransformerFactory createTransformerFactory() {
    TransformerFactory factory = TransformerFactory.newInstance();

    configureTransformerFactory(factory);

    return factory;
  }

  public SchemaFactory createSchemaFactory(String schemaLanguage) {
    SchemaFactory factory = SchemaFactory.newInstance(schemaLanguage);

    configureSchemaFactory(factory);

    return factory;
  }

  public void configureXMLInputFactory(XMLInputFactory factory) {
    factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, externalEntities);
    factory.setProperty(SUPPORT_DTD, expandEntities);
  }

  public void configureTransformerFactory(TransformerFactory factory) {
    if (!externalEntities && !expandEntities) {
      try {
        factory.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "");
        factory.setAttribute(ACCESS_EXTERNAL_DTD, "");
      } catch (Exception e) {
        logConfigurationWarning("TransformerFactory", factory.getClass().getName(), e);
      }
    }
  }

  public void configureSchemaFactory(SchemaFactory factory) {
    if (!externalEntities && !expandEntities) {
      try {
        factory.setProperty(ACCESS_EXTERNAL_STYLESHEET, "");
        factory.setProperty(ACCESS_EXTERNAL_DTD, "");
      } catch (Exception e) {
        logConfigurationWarning("SchemaFactory", factory.getClass().getName(), e);
      }
    }
  }

  public void configureValidator(Validator validator) {
    if (!externalEntities && !expandEntities) {
      try {
        validator.setProperty(ACCESS_EXTERNAL_STYLESHEET, "");
        validator.setProperty(ACCESS_EXTERNAL_DTD, "");
      } catch (Exception e) {
        logConfigurationWarning("Validator", validator.getClass().getName(), e);
      }
    }
  }

  protected static void logConfigurationWarning(String interfaceName, String implementationName, Throwable e) {
    logger.warn(format("Can't configure XML entity expansion for %s (%s), this could introduce XXE and BL vulnerabilities",
                       interfaceName, implementationName));
    logger.warn(e);
  }

  public Boolean getExternalEntities() {
    return externalEntities;
  }

  public Boolean getExpandEntities() {
    return expandEntities;
  }
}
