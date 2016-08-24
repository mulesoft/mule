/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class SimpleXmlTransformerFunctionalTestCase extends AbstractIntegrationTestCase {

  public static final String SERIALIZED = "<org.mule.test.xml.functional.SimpleXmlTransformerFunctionalTestCase_-Parent>\n"
      + "  <child>\n" + "    <name>theChild</name>\n" + "  </child>\n"
      + "</org.mule.test.xml.functional.SimpleXmlTransformerFunctionalTestCase_-Parent>";


  @Override
  protected String getConfigFile() {
    return "org/mule/module/xml/simple-xml-transformer-functional-test-flow.xml";
  }

  @Test
  public void testXmlOut() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("xml to object").withPayload(SERIALIZED).asynchronously().run();
    Parent parent = (Parent) request(client, "test://xml-object-out", Parent.class);
    assertNotNull(parent);
    assertNotNull(parent.getChild());
    assertThat(parent.getChild().getName(), is("theChild"));
  }

  @Test
  public void testObjectXmlOut() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("object to xml").withPayload(new Parent(new Child("theChild"))).asynchronously().run();
    String xml = (String) request(client, "test://object-xml-out", String.class);
    assertXMLEqual(SERIALIZED, xml);
  }

  protected Object request(MuleClient client, String endpoint, Class<?> clazz) throws MuleException {
    MuleMessage message = client.request(endpoint, RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(message);
    assertNotNull(message.getPayload());
    assertThat(message.getDataType().getType().getName(), message.getPayload(), instanceOf(clazz));
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

    private String name;

    public Child() {
      this(null);
    }

    public Child(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
