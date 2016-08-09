/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Rule;
import org.junit.Test;

public class HttpPersistentQueueTestCase extends FunctionalTestCase {

  private CountDownLatch messageDidArrive = new CountDownLatch(1);
  private int port = -1;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "http-persistent-queue-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("PersistentQueueAsync");
    assertNotNull(testComponent);
    testComponent.setEventCallback(new Callback(messageDidArrive));
    port = dynamicPort.getNumber();
  }

  @Test
  public void testPersistentMessageDeliveryWithGet() throws Exception {
    GetMethod method = new GetMethod("http://localhost:" + port + "/services/Echo?foo=bar");
    method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
    doTestPersistentMessageDelivery(method);
  }

  @Test
  public void testPersistentMessageDeliveryWithPost() throws Exception {
    PostMethod method = new PostMethod("http://localhost:" + port + "/services/Echo");
    method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
    method.addParameter(new NameValuePair("foo", "bar"));
    doTestPersistentMessageDelivery(method);
  }

  private void doTestPersistentMessageDelivery(HttpMethod httpMethod) throws Exception {
    HttpClient client = new HttpClient();
    int rc = client.executeMethod(httpMethod);

    assertEquals(HttpStatus.SC_OK, rc);
    assertTrue(messageDidArrive.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
  }

  private static class Callback implements EventCallback {

    private CountDownLatch messageDidArrive;

    public Callback(CountDownLatch latch) {
      super();
      messageDidArrive = latch;
    }

    @Override
    public void eventReceived(MuleEventContext context, Object component) throws Exception {
      MuleMessage message = context.getMessage();

      Object httpMethod = message.getInboundProperty("http.method");
      if (HttpConstants.METHOD_GET.equals(httpMethod)) {
        assertEquals("/services/Echo?foo=bar",
                     muleContext.getTransformationService().transform(message, DataType.STRING).getPayload());
      } else if (HttpConstants.METHOD_POST.equals(httpMethod)) {
        assertEquals("foo=bar", muleContext.getTransformationService().transform(message, DataType.STRING).getPayload());
      } else {
        fail("invalid HTTP method : " + httpMethod);
      }

      assertEquals("true", message.getInboundProperty(HttpConstants.HEADER_CONNECTION));
      assertEquals("true", message.getInboundProperty(HttpConstants.HEADER_KEEP_ALIVE));

      messageDidArrive.countDown();
    }
  }

}
