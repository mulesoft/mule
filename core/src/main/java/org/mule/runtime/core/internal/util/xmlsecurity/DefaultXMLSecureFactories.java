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
import javax.xml.stream.FactoryConfigurationError;
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

  public static final String DOCUMENT_BUILDER_FACTORY = "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
  public static final String SAX_PARSER_FACTORY = "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl";
  public static final String XML_INPUT_FACTORY = "com.sun.xml.internal.stream.XMLInputFactoryImpl";
  public static final String TRANSFORMER_FACTORY = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";
  public static final String SCHEMA_FACTORY = "com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory";

  public static final String DOCUMENT_BUILDER_PROPERTY = "javax.xml.parsers.DocumentBuilderFactory";
  public static final String SAX_PARSER_PROPERTY = "javax.xml.parsers.SAXParserFactory";
  public static final String XML_INPUT_PROPERTY = "javax.xml.stream.XMLInputFactory";
  public static final String TRANSFORMER_PROPERTY = "javax.xml.transform.TransformerFactory";
  public static final String SCHEMA_PROPERTY = "javax.xml.validation.SchemaFactory";

  private Boolean externalEntities;
  private Boolean expandEntities;

  private final static Log logger = LogFactory.getLog(DefaultXMLSecureFactories.class);

  public DefaultXMLSecureFactories(Boolean externalEntities, Boolean expandEntities) {
    this.externalEntities = externalEntities;
    this.expandEntities = expandEntities;
  }

  public DocumentBuilderFactory createDocumentBuilderFactory() {
    DocumentBuilderFactory factory;

    if (System.getProperty(DOCUMENT_BUILDER_PROPERTY) == null) {
      try {
        factory = DocumentBuilderFactory.newInstance(DOCUMENT_BUILDER_FACTORY, DefaultXMLSecureFactories.class.getClassLoader());
      } catch (FactoryConfigurationError e) {
        logCreationWarning(DocumentBuilderFactory.class.getName(), DOCUMENT_BUILDER_FACTORY, e);
        factory = DocumentBuilderFactory.newInstance();
      }
    } else {
      factory = DocumentBuilderFactory.newInstance();
    }

    try {
      factory.setFeature("http://xml.org/sax/features/external-general-entities", externalEntities);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", externalEntities);
      factory.setExpandEntityReferences(expandEntities);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !expandEntities);
    } catch (Exception e) {
      logConfigurationWarning(DocumentBuilderFactory.class.getName(), factory.getClass().getName(), e);
    }

    return factory;
  }

  public SAXParserFactory createSaxParserFactory() {
    SAXParserFactory factory;

    if (System.getProperty(SAX_PARSER_PROPERTY) == null) {
      try {
        factory = SAXParserFactory.newInstance(SAX_PARSER_FACTORY, DefaultXMLSecureFactories.class.getClassLoader());
      } catch (FactoryConfigurationError e) {
        logCreationWarning(SAXParserFactory.class.getName(), SAX_PARSER_FACTORY, e);
        factory = SAXParserFactory.newInstance();
      }
    } else {
      factory = SAXParserFactory.newInstance();
    }

    try {
      factory.setFeature("http://xml.org/sax/features/external-general-entities", externalEntities);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", externalEntities);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !expandEntities);
    } catch (Exception e) {
      logConfigurationWarning(SAXParserFactory.class.getName(), factory.getClass().getName(), e);
    }

    return factory;
  }

  public XMLInputFactory createXMLInputFactory() {
    XMLInputFactory factory;

    if (System.getProperty(XML_INPUT_PROPERTY) == null) {
      try {
        // There is no way to pass the class without an intermediate system property
        final String propertyName = "_mule.XMLInputFactory";

        System.setProperty(propertyName, XML_INPUT_FACTORY);
        factory = XMLInputFactory.newFactory(propertyName, DefaultXMLSecureFactories.class.getClassLoader());
      } catch (FactoryConfigurationError e) {
        logCreationWarning(XMLInputFactory.class.getName(), XML_INPUT_FACTORY, e);
        factory = XMLInputFactory.newInstance();
      }
    } else {
      factory = XMLInputFactory.newInstance();
    }

    configureXMLInputFactory(factory);

    return factory;
  }

  public TransformerFactory createTransformerFactory() {
    TransformerFactory factory;

    if (System.getProperty(TRANSFORMER_PROPERTY) == null) {
      try {
        factory = TransformerFactory.newInstance(TRANSFORMER_FACTORY, DefaultXMLSecureFactories.class.getClassLoader());
      } catch (FactoryConfigurationError e) {
        logCreationWarning(TransformerFactory.class.getName(), TRANSFORMER_FACTORY, e);
        factory = TransformerFactory.newInstance();
      }
    } else {
      factory = TransformerFactory.newInstance();
    }

    configureTransformerFactory(factory);

    return factory;
  }

  public SchemaFactory createSchemaFactory(String schemaLanguage) {
    String schemaProperty = SCHEMA_PROPERTY + ":" + schemaLanguage;
    SchemaFactory factory;

    if (System.getProperty(schemaProperty) == null) {
      try {
        factory = SchemaFactory.newInstance(schemaLanguage, SCHEMA_FACTORY, DefaultXMLSecureFactories.class.getClassLoader());
      } catch (IllegalArgumentException e) {
        logCreationWarning(SchemaFactory.class.getName(), SCHEMA_FACTORY, e);
        factory = SchemaFactory.newInstance(schemaLanguage);
      }
    } else {
      factory = SchemaFactory.newInstance(schemaLanguage);
    }

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
        logConfigurationWarning(TransformerFactory.class.getName(), factory.getClass().getName(), e);
      }
    }
  }

  public void configureSchemaFactory(SchemaFactory factory) {
    if (!externalEntities && !expandEntities) {
      try {
        factory.setProperty(ACCESS_EXTERNAL_STYLESHEET, "");
        factory.setProperty(ACCESS_EXTERNAL_DTD, "");
      } catch (Exception e) {
        logConfigurationWarning(SchemaFactory.class.getName(), factory.getClass().getName(), e);
      }
    }
  }

  public void configureValidator(Validator validator) {
    if (!externalEntities && !expandEntities) {
      try {
        validator.setProperty(ACCESS_EXTERNAL_STYLESHEET, "");
        validator.setProperty(ACCESS_EXTERNAL_DTD, "");
      } catch (Exception e) {
        logConfigurationWarning(Validator.class.getName(), validator.getClass().getName(), e);
      }
    }
  }

  protected static void logConfigurationWarning(String interfaceName, String implementationName, Throwable e) {
    logger.warn(format("Can't configure XML entity expansion for %s (%s), this could introduce XXE and BL vulnerabilities",
                       interfaceName, implementationName),
                e);
  }

  protected static void logCreationWarning(String interfaceName, String desiredImplementation, Throwable e) {
    logger.warn(format("Can't create %s (%s), falling back to default implementation", interfaceName, desiredImplementation), e);
  }

  public Boolean getExternalEntities() {
    return externalEntities;
  }

  public Boolean getExpandEntities() {
    return expandEntities;
  }
}
