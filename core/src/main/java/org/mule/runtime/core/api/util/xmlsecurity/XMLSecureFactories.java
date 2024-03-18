/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.xmlsecurity;

import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Provide XML parser factories configured to avoid XXE and BL attacks according to global configuration (safe by default)
 *
 * @deprecated since 4.4.0 Use {@link org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories} instead
 */
@Deprecated
public final class XMLSecureFactories {

  public static final String EXTERNAL_ENTITIES_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + "xml.expandExternalEntities";
  public static final String EXPAND_ENTITIES_PROPERTY =
      SYSTEM_PROPERTY_PREFIX + "xml.expandInternalEntities";

  private final org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories delegate;

  public static XMLSecureFactories createWithConfig(Boolean externalEntities, Boolean expandEntities) {
    return new XMLSecureFactories(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createWithConfig(externalEntities,
                                                                                                            expandEntities));
  }

  public static XMLSecureFactories createDefault() {
    return new XMLSecureFactories(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createDefault());
  }

  private XMLSecureFactories(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories delegate) {
    this.delegate = delegate;
  }

  public DocumentBuilderFactory getDocumentBuilderFactory() {
    return delegate.getDocumentBuilderFactory();
  }

  public SAXParserFactory getSAXParserFactory() {
    return delegate.getSAXParserFactory();
  }

  public XMLInputFactory getXMLInputFactory() {
    return delegate.getXMLInputFactory();
  }

  public TransformerFactory getTransformerFactory() {
    return delegate.getTransformerFactory();
  }

  public SchemaFactory getSchemaFactory(String schemaLocation) {
    return delegate.getSchemaFactory(schemaLocation);
  }

  public void configureXMLInputFactory(XMLInputFactory factory) {
    delegate.configureXMLInputFactory(factory);
  }

  public void configureTransformerFactory(TransformerFactory factory) {
    delegate.configureTransformerFactory(factory);
  }

  public void configureValidator(Validator validator) {
    delegate.configureValidator(validator);
  }
}
