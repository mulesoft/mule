/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.xmlsecurity;

import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.runtime.core.internal.util.xmlsecurity.DefaultXMLSecureFactories;
import org.mule.runtime.core.internal.util.xmlsecurity.XMLSecureFactoriesCache;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Provide XML parser factories configured to avoid XXE and BL attacks according to global configuration (safe by default)
 */
public class XMLSecureFactories {

  public static final String EXTERNAL_ENTITIES_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + "xml.expandExternalEntities";
  public static final String EXPAND_ENTITIES_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + "xml.expandInternalEntities";

  private Boolean externalEntities;
  private Boolean expandEntities;
  private DefaultXMLSecureFactories secureFactories;

  public static XMLSecureFactories createWithConfig(Boolean externalEntities, Boolean expandEntities) {
    return new XMLSecureFactories(externalEntities, expandEntities);
  }

  public static XMLSecureFactories createDefault() {
    return new XMLSecureFactories();
  }


  private XMLSecureFactories() {
    String externalEntitiesValue = System.getProperty(EXTERNAL_ENTITIES_PROPERTY, "false");
    externalEntities = Boolean.parseBoolean(externalEntitiesValue);

    String expandEntitiesValue = System.getProperty(EXPAND_ENTITIES_PROPERTY, "false");
    expandEntities = Boolean.parseBoolean(expandEntitiesValue);

    secureFactories = new DefaultXMLSecureFactories(externalEntities, expandEntities);
  }

  private XMLSecureFactories(Boolean externalEntities, Boolean expandEntities) {
    this.externalEntities = externalEntities;
    this.expandEntities = expandEntities;
    this.secureFactories = new DefaultXMLSecureFactories(externalEntities, expandEntities);
  }

  public DocumentBuilderFactory getDocumentBuilderFactory() {
    return XMLSecureFactoriesCache.getInstance().getDocumentBuilderFactory(secureFactories);
  }

  public SAXParserFactory getSAXParserFactory() {
    return XMLSecureFactoriesCache.getInstance().getSAXParserFactory(secureFactories);
  }

  public XMLInputFactory getXMLInputFactory() {
    return XMLSecureFactoriesCache.getInstance().getXMLInputFactory(secureFactories);
  }

  public TransformerFactory getTransformerFactory() {
    return XMLSecureFactoriesCache.getInstance().getTransformerFactory(secureFactories);
  }

  public SchemaFactory getSchemaFactory(String schemaLocation) {
    return XMLSecureFactoriesCache.getInstance().getSchemaFactory(secureFactories, schemaLocation);
  }

  public void configureXMLInputFactory(XMLInputFactory factory) {
    secureFactories.configureXMLInputFactory(factory);
  }

  public void configureTransformerFactory(TransformerFactory factory) {
    secureFactories.configureTransformerFactory(factory);
  }

  public void configureValidator(Validator validator) {
    secureFactories.configureValidator(validator);
  }
}
