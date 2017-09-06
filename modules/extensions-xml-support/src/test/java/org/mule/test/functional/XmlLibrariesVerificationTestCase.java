/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

/**
 * Make sure third party libraries in the container can't override default Java factories by SPI with META-INF/services.
 */
@Feature("Security")
@Story("Provide correct XML factories with isolation")
public class XmlLibrariesVerificationTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "modules/module-simple.xml";
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-simple.xml";
  }

  @Test
  public void documentBuilder() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"));
  }

  @Test
  public void saxParser() {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"));
  }

  @Test
  public void xmlInput() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    assertThat(factory.getClass().getName(), equalTo("com.sun.xml.internal.stream.XMLInputFactoryImpl"));
  }

  @Test
  public void transformer() {
    TransformerFactory factory = TransformerFactory.newInstance();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"));
  }

  @Test
  public void schema() {
    SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory"));
  }
}
