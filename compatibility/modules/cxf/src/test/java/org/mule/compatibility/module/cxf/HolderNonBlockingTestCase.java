/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.mule.service.http.api.HttpConstants.Method.POST;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.SensingNullRequestResponseMessageProcessor;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ObjectInputStream;
import java.nio.charset.Charset;

import javax.xml.ws.Holder;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-10618")
public class HolderNonBlockingTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String getConfigFile() {
    return "holder-conf-httpn-nb.xml";
  }

  @Test
  @Ignore("MULE-10618")
  public void testClientEchoHolder() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/echoClient")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity("TEST".getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Object[] payload = deserializeResponse(response);
    assertEquals("one-response", payload[0]);
    assertEquals(null, payload[1]);
    assertEquals("one-holder1", ((Holder) payload[2]).value);
    assertEquals("one-holder2", ((Holder) payload[3]).value);
    getSensingInstance("sensingRequestResponseProcessorEcho").assertRequestResponseThreadsDifferent();
  }

  @Test
  @Ignore("MULE-10618")
  public void testClientProxyEchoHolder() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/echoClientProxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity("TEST".getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Object[] payload = deserializeResponse(response);
    assertEquals("one-response", payload[0]);
    assertEquals("one-holder1", ((Holder) payload[1]).value);
    assertEquals("one-holder2", ((Holder) payload[2]).value);
    getSensingInstance("sensingRequestResponseProcessorEchoProxy").assertRequestResponseThreadsSame();
  }

  @Test
  @Ignore("MULE-10618")
  public void testClientEcho2Holder() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/echo2Client")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity("TEST".getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Object[] payload = deserializeResponse(response);
    assertEquals("one-response", payload[0]);
    assertEquals(null, payload[1]);
    assertEquals("two-holder", ((Holder) payload[2]).value);
    getSensingInstance("sensingRequestResponseProcessorEcho2").assertRequestResponseThreadsDifferent();
  }

  @Test
  @Ignore("MULE-10618")
  public void testClientProxyEcho2Holder() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/echo2ClientProxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity("TEST".getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Object[] payload = deserializeResponse(response);
    assertEquals("one-response", payload[0]);
    assertEquals("two-holder", ((Holder) payload[1]).value);
    getSensingInstance("sensingRequestResponseProcessorEchoProxy2").assertRequestResponseThreadsSame();
  }

  @Test
  @Ignore("MULE-10618")
  public void testClientEcho3Holder() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/echo3Client")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity("TEST".getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Object[] payload = deserializeResponse(response);

    assertEquals(null, payload[0]);
    assertEquals("one", ((Holder) payload[1]).value);
    getSensingInstance("sensingRequestResponseProcessorEcho3").assertRequestResponseThreadsDifferent();
  }

  @Test
  @Ignore("MULE-10618")
  public void testClientProxyEcho3Holder() throws Exception {
    HttpRequest request =
        HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber() + "/echo3ClientProxy")
            .setMethod(POST)
            .setEntity(new ByteArrayHttpEntity("TEST".getBytes())).build();

    HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    Object[] payload = deserializeResponse(response);
    assertEquals(null, payload[0]);
    assertEquals("one", ((Holder) payload[1]).value);
    getSensingInstance("sensingRequestResponseProcessorEchoProxy3").assertRequestResponseThreadsSame();
  }

  private SensingNullRequestResponseMessageProcessor getSensingInstance(String instanceBeanName) {
    return ((SensingNullRequestResponseMessageProcessor) muleContext.getRegistry().lookupObject(instanceBeanName));
  }

  private static <T> T deserializeResponse(HttpResponse received) throws Exception {
    ObjectInputStream objectInputStream = new ObjectInputStream(((InputStreamHttpEntity) received.getEntity()).getInputStream());
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
