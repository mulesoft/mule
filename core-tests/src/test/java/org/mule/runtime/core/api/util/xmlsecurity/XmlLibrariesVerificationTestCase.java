/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.xmlsecurity;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.util.xmlsecurity.XMLSecureFactories.createDefault;

import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;

/**
 * The asserted classes were obtained from getting the factories in a java component in a standalone mule.
 *
 * We can't override the default factories because Woodstox and Saxon register service providers with
 * their own implementations (in META-INF/services).
 */
public class XmlLibrariesVerificationTestCase extends AbstractMuleTestCase {

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

  @Test
  public void saxonTransformer() {
    TransformerFactory factory = createDefault().getSaxonTransformerFactory();
    assertThat(factory.getClass().getName(), equalTo("net.sf.saxon.jaxp.SaxonTransformerFactory"));
  }
}
