/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories.createDefault;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;

/**
 * Check that secure factories use Java defaults, which are more updated and support the latest security features.
 */
public class XmlSecureLibrariesVerificationTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

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
    DocumentBuilderFactory factory = createDefault().getDocumentBuilderFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl"));
  }

  @Test
  public void saxParser() {
    SAXParserFactory factory = createDefault().getSAXParserFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"));
  }

  @Test
  public void xmlInput() {
    XMLInputFactory factory = createDefault().getXMLInputFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.xml.internal.stream.XMLInputFactoryImpl"));
  }

  @Test
  public void transformer() {
    TransformerFactory factory = createDefault().getTransformerFactory();
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"));
  }

  @Test
  public void schema() {
    SchemaFactory factory = createDefault().getSchemaFactory("http://www.w3.org/2001/XMLSchema");
    assertThat(factory.getClass().getName(), equalTo("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory"));
  }
}
