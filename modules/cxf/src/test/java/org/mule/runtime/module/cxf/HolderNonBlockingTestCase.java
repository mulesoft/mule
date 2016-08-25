/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;

import javax.xml.ws.Holder;

import org.junit.Rule;
import org.junit.Test;

public class HolderNonBlockingTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "holder-conf-httpn-nb.xml";
  }

  @Test
  public void testClientEchoHolder() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("TEST").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/echoClient", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    assertNotNull(received);
    Object[] payload = deserializeResponse(received);
    assertEquals("one-response", payload[0]);
    assertEquals(null, payload[1]);
    assertEquals("one-holder1", ((Holder) payload[2]).value);
    assertEquals("one-holder2", ((Holder) payload[3]).value);
    getSensingInstance("sensingRequestResponseProcessorEcho").assertRequestResponseThreadsDifferent();
  }

  @Test
  public void testClientProxyEchoHolder() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("TEST").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/echoClientProxy", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    assertNotNull(received);
    Object[] payload = deserializeResponse(received);
    assertEquals("one-response", payload[0]);
    assertEquals("one-holder1", ((Holder) payload[1]).value);
    assertEquals("one-holder2", ((Holder) payload[2]).value);
    getSensingInstance("sensingRequestResponseProcessorEchoProxy").assertRequestResponseThreadsSame();
  }

  @Test
  public void testClientEcho2Holder() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("TEST").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo2Client", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    assertNotNull(received);
    Object[] payload = deserializeResponse(received);
    assertEquals("one-response", payload[0]);
    assertEquals(null, payload[1]);
    assertEquals("two-holder", ((Holder) payload[2]).value);
    getSensingInstance("sensingRequestResponseProcessorEcho2").assertRequestResponseThreadsDifferent();
  }

  @Test
  public void testClientProxyEcho2Holder() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("TEST").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo2ClientProxy", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    assertNotNull(received);
    Object[] payload = deserializeResponse(received);
    assertEquals("one-response", payload[0]);
    assertEquals("two-holder", ((Holder) payload[1]).value);
    getSensingInstance("sensingRequestResponseProcessorEchoProxy2").assertRequestResponseThreadsSame();
  }

  @Test
  public void testClientEcho3Holder() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("TEST").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo3Client", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    assertNotNull(received);
    Object[] payload = deserializeResponse(received);
    assertEquals(null, payload[0]);
    assertEquals("one", ((Holder) payload[1]).value);
    getSensingInstance("sensingRequestResponseProcessorEcho3").assertRequestResponseThreadsDifferent();
  }

  @Test
  public void testClientProxyEcho3Holder() throws Exception {
    MuleMessage request = MuleMessage.builder().payload("TEST").build();
    MuleClient client = muleContext.getClient();
    MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo3ClientProxy", request,
                                       newOptions().method(POST.name()).disableStatusCodeValidation().build())
        .getRight();
    assertNotNull(received);
    Object[] payload = deserializeResponse(received);
    assertEquals(null, payload[0]);
    assertEquals("one", ((Holder) payload[1]).value);
    getSensingInstance("sensingRequestResponseProcessorEchoProxy3").assertRequestResponseThreadsSame();
  }

  private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName) {
    return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
  }

  private static <T> T deserializeResponse(MuleMessage received) throws Exception {
    ObjectInputStream objectInputStream = new ObjectInputStream((InputStream) received.getPayload());
    Object objectPayload = objectInputStream.readObject();
    return (T) objectPayload;
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
