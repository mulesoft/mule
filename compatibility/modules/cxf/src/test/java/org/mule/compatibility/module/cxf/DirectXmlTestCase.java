/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.message.InternalMessage;
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

public class DirectXmlTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");


  @Override
  protected String getConfigFile() {
    return "direct/direct-xml-conf-flow.xml";
  }

  @Test
  public void testInputStream() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    assertThat(xml, not(nullValue()));

    test(xml);
  }

  @Ignore("MULE-9285")
  @Test
  public void testInputStreamWithXslt() throws Exception {
    InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
    assertNotNull(xml);

    InternalMessage result = flowRunner("echoWithTransform").withPayload(xml).run().getMessage();
    String resultStr = getPayloadAsString(result);
    assertThat("echoResponse not found in result: " + resultStr, resultStr, containsString("echoResponse"));
  }

  private void test(Object xml) throws Exception {
    InternalMessage result = flowRunner("echoService").withPayload(xml).run().getMessage();
    assertThat(getPayloadAsString(result), containsString("echoResponse"));
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
