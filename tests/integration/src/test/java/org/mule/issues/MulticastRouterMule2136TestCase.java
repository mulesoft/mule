/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.xml.functional.AbstractXmlFunctionalTestCase;
import org.mule.test.xml.functional.XmlTransformerFunctionalTestCase;

import org.junit.Test;

/**
 * This is a simplified version of {@link XmlTransformerFunctionalTestCase} The {@link #testObjectXmlOut()} method hangs
 * intermittently.
 */
public class MulticastRouterMule2136TestCase extends AbstractXmlFunctionalTestCase {

  public static final int TEST_COUNT = 2000; // cut down from 10k messages, since
                                             // it seems a little much for the
                                             // continuous build
  public static final String SERIALIZED = "<org.mule.issues.MulticastRouterMule2136TestCase_-Parent>\n" + "  <child/>\n"
      + "</org.mule.issues.MulticastRouterMule2136TestCase_-Parent>";

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/multicast-router-mule-2136-test-flow.xml";
  }

  protected void sendObject() throws Exception {
    flowRunner("object to xml").withPayload(new Parent(new Child())).asynchronously().run();
  }

  @Test
  public void testObjectOut() throws Exception {
    sendObject();
    request("test://object-out", Parent.class);
    // wait a while, otherwise we pull down everything while it is still running
    Thread.sleep(3000);
  }

  @Test
  public void testObjectXmlOut() throws Exception {
    sendObject();
    String xml = (String) request("test://object-xml-out", String.class);
    assertThat(xml, is(SERIALIZED));
  }

  @Test
  public void testXmlObjectOut() throws Exception {
    sendObject();
    request("test://xml-object-out", Parent.class);
  }

  @Test
  public void testStress() throws Exception {
    int tenth = TEST_COUNT / 10;
    for (int i = 0; i < TEST_COUNT; i++) {
      testObjectXmlOut();

      // Pull result from "xml-object-out" endpoint as queuing is enabled and
      // otherwise we get
      // OutOfMemoryExceptions during stress tests when these results build up
      // in queue.
      request("test://xml-object-out", Parent.class);
      request("test://object-out", Parent.class);

      logger.error("Iteration " + i);
      if (i % tenth == 0) {
        logger.info("Iteration " + i);
      }
    }
  }

  protected Object request(String endpoint, Class<?> clazz) throws MuleException {
    MuleClient client = muleContext.getClient();

    MuleMessage message = client.request(endpoint, TIMEOUT * 2).getRight().get();
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
