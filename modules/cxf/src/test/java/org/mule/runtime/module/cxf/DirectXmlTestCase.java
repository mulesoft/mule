/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.xml.stax.StaxSource;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import org.apache.cxf.helpers.DOMUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class DirectXmlTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");


  @Override
  protected String getConfigFile() {
    return "direct/direct-xml-conf-flow.xml";
  }

  @Test
  public void testInputStream() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    assertNotNull(xml);

    test(xml);
  }

  @Ignore("MULE-9285")
  @Test
  public void testInputStreamWithXslt() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    assertNotNull(xml);

    MuleMessage result = flowRunner("echoWithTransform").withPayload(xml).run().getMessage();
    String resultStr = getPayloadAsString(result);
    assertTrue("echoResponse not found in result: " + resultStr, resultStr.indexOf("echoResponse") != -1);
  }

  private void test(Object xml) throws Exception {
    MuleMessage result = flowRunner("echoService").withPayload(xml).run().getMessage();
    assertTrue(getPayloadAsString(result).indexOf("echoResponse") != -1);
  }

  @Test
  public void testDom() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    Document dom = DOMUtils.readXml(xml);
    test(dom);
  }

  @Test
  public void testDomSource() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    Document dom = DOMUtils.readXml(xml);
    test(new DOMSource(dom));
  }

  @Test
  public void testSAXSource() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    SAXSource source = new SAXSource(new InputSource(xml));
    test(source);
  }

  @Test
  public void testStaxSource() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

    XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xml);
    test(new StaxSource(reader));
  }

  @Test
  public void testXMLStreamReader() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");

    XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(xml);
    test(reader);
  }
}
