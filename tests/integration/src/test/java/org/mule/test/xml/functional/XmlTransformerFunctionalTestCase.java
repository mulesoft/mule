/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.w3c.dom.Document;

public class XmlTransformerFunctionalTestCase extends AbstractXmlFunctionalTestCase {

  public static final String SIMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<parent><child name=\"poot\"/></parent>";
  public static final String CHILDLESS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<parent/>";
  public static final String SERIALIZED = "<org.mule.test.xml.functional.XmlTransformerFunctionalTestCase_-Parent>\n"
      + "  <child/>\n" + "</org.mule.test.xml.functional.XmlTransformerFunctionalTestCase_-Parent>";

  @Override
  protected String getConfigFile() {
    return "org/mule/module/xml/xml-transformer-functional-test-flow.xml";
  }

  protected void sendXml() throws Exception {
    flowRunner("xml to ...").withPayload(SIMPLE_XML).asynchronously().run();
  }

  protected void sendObject() throws Exception {
    sendObject("object to xml");
  }

  protected void sendObject(String flowName) throws Exception {
    flowRunner(flowName).withPayload(new Parent(new Child())).asynchronously().run();
  }

  @Test
  public void testXmlOut() throws Exception {
    sendXml();
    String xml = (String) request("test://xml-out", String.class);
    assertXMLEqual(SIMPLE_XML, xml);
  }

  @Test
  public void testXmlDomOut() throws Exception {
    sendXml();
    Document dom = (Document) request("test://xml-dom-out", Document.class);
    assertEquals("parent", dom.getDocumentElement().getLocalName());
  }

  @Test
  public void testXmlXsltOut() throws Exception {
    sendXml();
    String xml = (String) request("test://xml-xslt-out-string", String.class);
    assertXMLEqual(CHILDLESS_XML, xml);
  }

  @Test
  public void testDomXmlOut() throws Exception {
    sendXml();
    String xml = (String) request("test://dom-xml-out", String.class);
    XMLAssert.assertXMLEqual(SIMPLE_XML, xml);
  }

  @Test
  public void testObjectOut() throws Exception {
    sendObject();
    request("test://object-out", Parent.class);
  }

  @Test
  public void testObjectXmlOut() throws Exception {
    sendObject();
    String xml = (String) request("test://object-xml-out", String.class);
    System.out.println(xml);
    XMLAssert.assertXMLEqual(SERIALIZED, xml);
  }

  protected Object request(String endpoint, Class<?> clazz) throws MuleException {
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.request(endpoint, TIMEOUT).getRight().get();
    assertNotNull(message);
    assertNotNull(message.getPayload());
    assertThat(message.getPayload(), instanceOf(clazz));
    return message.getPayload();
  }

  public static class Parent {

    private Child child;

    public Parent() {
      this(null);
    }

    public Parent(Child child) {
      setChild(child);
    }

    public Child getChild() {
      return child;
    }

    public void setChild(Child child) {
      this.child = child;
    }
  }

  public static class Child {
    // nothing here
  }
}
