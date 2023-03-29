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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;

import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.Validator;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XMLSecureFactoriesPropertiesTestCase {

  private static final String SCHEMA_LOCATION = "http://www.w3.org/2001/XMLSchema";

  private org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories delegate;

  @Before
  public void setup() {
    mockStatic(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.class);
    delegate = mock(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.class);
  }

  @Test
  @Issue("MULE-18814")
  public void decorateDefaultXmlSecureFactories() {
    given(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createDefault()).willReturn(delegate);
    XMLSecureFactories deprecated = XMLSecureFactories.createDefault();

    assertDecoratedXMLSecureFactories(deprecated);
  }

  @Test
  @Issue("MULE-18814")
  public void decorateCustomXmlSecureFactories() {
    given(org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createWithConfig(anyBoolean(), anyBoolean()))
        .willReturn(delegate);
    XMLSecureFactories deprecated = XMLSecureFactories.createWithConfig(false, false);

    assertDecoratedXMLSecureFactories(deprecated);
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
