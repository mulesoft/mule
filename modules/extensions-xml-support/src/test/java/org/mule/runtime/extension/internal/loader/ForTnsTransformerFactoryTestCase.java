/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class ForTnsTransformerFactoryTestCase extends AbstractMuleTestCase {

  @Test
  @Description("Test the xslt transformer that prepares the module definition for the extension model generation")
  public void forTnsTransformation() throws TransformerException, ParserConfigurationException, SAXException, IOException {
    final ForTnsTransformerFactory transformerFactory = new ForTnsTransformerFactory();
    final ByteArrayOutputStream result = new ByteArrayOutputStream();
    transformerFactory.create()
        .transform(new StreamSource(ForTnsTransformerFactoryTestCase.class.getResourceAsStream("/modules/module-simple.xml")),
                   new StreamResult(result));

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    final Document transformedDom = builder.parse(result.toInputStream());

    final Element root = transformedDom.getDocumentElement();
    assertThat(root.getNodeName(), is("module"));
    final Node firstOperation = root.getElementsByTagName("operation").item(0);
    assertThat(firstOperation, not(nullValue()));
    // body was removed...
    assertThat(((Element) firstOperation).getElementsByTagName("body").item(0).getChildNodes().getLength(), is(0));
    // ... and output is kept
    assertThat(((Element) firstOperation).getElementsByTagName("output").item(0), not(nullValue()));
  }
}
