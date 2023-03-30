/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.xmlsecurity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.Validator;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XMLSecureFactoriesPropertiesTestCase {

  private static final String SCHEMA_LOCATION = "http://www.w3.org/2001/XMLSchema";

  private org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories delegate;

  @Test
  @Issue("MULE-18814")
  public void decorateDefaultXmlSecureFactories() {
    MockedStatic<org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories> utilities =
        Mockito.mockStatic(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.class);
    try {
      delegate = spy(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.class);
      utilities.when(() -> XMLSecureFactories.createDefault()).thenReturn(delegate);
      XMLSecureFactories deprecated = XMLSecureFactories.createDefault();

      assertDecoratedXMLSecureFactories(deprecated);
    } finally {
      utilities.close();
    }
  }

  @Test
  @Issue("MULE-18814")
  public void decorateCustomXmlSecureFactories() {
    MockedStatic<org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories> utilities =
        Mockito.mockStatic(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.class);
    try {
      delegate = spy(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.class);
      utilities.when(() -> XMLSecureFactories.createWithConfig(anyBoolean(), anyBoolean())).thenReturn(delegate);
      XMLSecureFactories deprecated = XMLSecureFactories.createWithConfig(false, false);

      assertDecoratedXMLSecureFactories(deprecated);
    } finally {
      utilities.close();
    }
  }

  private void assertDecoratedXMLSecureFactories(XMLSecureFactories deprecated) {
    assertThat(deprecated, is(notNullValue()));

    deprecated.getDocumentBuilderFactory();
    verify(delegate).getDocumentBuilderFactory();

    deprecated.getSAXParserFactory();
    verify(delegate).getSAXParserFactory();

    deprecated.getXMLInputFactory();
    verify(delegate).getXMLInputFactory();

    deprecated.getTransformerFactory();
    verify(delegate).getTransformerFactory();

    deprecated.getSchemaFactory(SCHEMA_LOCATION);
    verify(delegate).getSchemaFactory(SCHEMA_LOCATION);


    XMLInputFactory xmlInputFactory = mock(XMLInputFactory.class);
    deprecated.configureXMLInputFactory(xmlInputFactory);
    verify(delegate).configureXMLInputFactory(xmlInputFactory);

    TransformerFactory transformerFactory = mock(TransformerFactory.class);
    deprecated.configureTransformerFactory(transformerFactory);
    verify(delegate).configureTransformerFactory(transformerFactory);

    Validator validator = mock(Validator.class);
    deprecated.configureValidator(validator);
    verify(delegate).configureValidator(validator);
  }
}
