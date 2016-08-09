/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.tck.junit4.rule.DynamicPort;

import java.nio.charset.Charset;

import javax.xml.ws.Holder;

import org.junit.Rule;
import org.junit.Test;

public class HolderTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "holder-conf-httpn.xml";
  }

  @Test
  public void testClientEchoHolder() throws Exception {
    MuleMessage received = flowRunner("echoServiceClient").withPayload(getTestMuleMessage(TEST_PAYLOAD)).run().getMessage();
    assertNotNull(received);
    Object[] payload = (Object[]) received.getPayload();
    assertEquals("one-response", payload[0]);
    assertEquals(null, payload[1]);
    assertEquals("one-holder1", ((Holder) payload[2]).value);
    assertEquals("one-holder2", ((Holder) payload[3]).value);
  }

  @Test
  public void testClientProxyEchoHolder() throws Exception {
    MuleMessage received = flowRunner("echoServiceClientProxy").withPayload(getTestMuleMessage(TEST_PAYLOAD)).run().getMessage();
    assertNotNull(received);
    Object[] payload = (Object[]) received.getPayload();
    assertEquals("one-response", payload[0]);
    assertEquals("one-holder1", ((Holder) payload[1]).value);
    assertEquals("one-holder2", ((Holder) payload[2]).value);
  }

  @Test
  public void testClientEcho2Holder() throws Exception {
    MuleMessage received = flowRunner("echo2ServiceClient").withPayload(getTestMuleMessage(TEST_PAYLOAD)).run().getMessage();
    assertNotNull(received);
    Object[] payload = (Object[]) received.getPayload();
    assertEquals("one-response", payload[0]);
    assertEquals(null, payload[1]);
    assertEquals("two-holder", ((Holder) payload[2]).value);
  }

  @Test
  public void testClientProxyEcho2Holder() throws Exception {
    MuleMessage received = flowRunner("echo2ServiceClientProxy").withPayload(getTestMuleMessage(TEST_PAYLOAD)).run().getMessage();
    assertNotNull(received);
    Object[] payload = (Object[]) received.getPayload();
    assertEquals("one-response", payload[0]);
    assertEquals("two-holder", ((Holder) payload[1]).value);
  }

  @Test
  public void testClientEcho3Holder() throws Exception {
    MuleMessage received = flowRunner("echo3ServiceClient").withPayload(getTestMuleMessage(TEST_PAYLOAD)).run().getMessage();
    assertNotNull(received);
    Object[] payload = (Object[]) received.getPayload();
    assertEquals(null, payload[0]);
    assertEquals("one", ((Holder) payload[1]).value);
  }

  @Test
  public void testClientProxyEcho3Holder() throws Exception {
    MuleMessage received = flowRunner("echo3ServiceClientProxy").withPayload(getTestMuleMessage(TEST_PAYLOAD)).run().getMessage();
    assertNotNull(received);
    Object[] payload = (Object[]) received.getPayload();
    assertEquals(null, payload[0]);
    assertEquals("one", ((Holder) payload[1]).value);
  }

  public static class HolderTransformer extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      Holder<String> outS1 = new Holder<>();
      Holder<String> outS2 = new Holder<>();

      Object objArray[] = new Object[3];
      objArray[0] = "one";
      objArray[1] = outS1;
      objArray[2] = outS2;

      return objArray;
    }
  }

  public static class HolderTransformer2 extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      Holder<String> outS1 = new Holder<>();

      Object objArray[] = new Object[3];
      objArray[0] = "one";
      objArray[1] = outS1;
      objArray[2] = "two";

      return objArray;
    }
  }

  public static class HolderTransformer3 extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      Holder<String> outS1 = new Holder<>();

      Object objArray[] = new Object[2];
      objArray[0] = outS1;
      objArray[1] = "one";

      return objArray;
    }
  }
}
