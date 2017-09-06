/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

/**
 * Make sure our XML factories provide the Java defaults independently of the classloader.
 */
@Feature("Security")
@Story("Provide correct XML factories even without isolation")
public class XmlSecureLibrariesVerificationTestCase extends AbstractMuleTestCase {

  @Test
  public void documentBuilder() {
    DocumentBuilderFactory factory = XMLSecureFactories.createDefault().getDocumentBuilderFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"));
  }

  @Test
  public void saxParser() {
    SAXParserFactory factory = XMLSecureFactories.createDefault().getSAXParserFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"));
  }

  @Test
  public void xmlInput() {
    XMLInputFactory factory = XMLSecureFactories.createDefault().getXMLInputFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.xml.internal.stream.XMLInputFactoryImpl"));
  }

  @Test
  public void transformer() {
    TransformerFactory factory = XMLSecureFactories.createDefault().getTransformerFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"));
  }

  @Test
  public void schema() {
    SchemaFactory factory = XMLSecureFactories.createDefault().getSchemaFactory("http://www.w3.org/2001/XMLSchema");
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory"));
  }
}
