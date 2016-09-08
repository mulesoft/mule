/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.junit.Rule;
import org.junit.Test;

public class HttpMessageReceiverMule4456TestCase extends FunctionalTestCase {

  private static final String MESSAGE = "test message";

  private HttpClient httpClient;
  private MuleClient muleClient;

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "http-receiver-mule4456-config-flow.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    HttpClientParams params = new HttpClientParams();
    params.setVersion(HttpVersion.HTTP_1_1);
    httpClient = new HttpClient(params);
    muleClient = muleContext.getClient();
  }

  @Test
  public void testAsyncPost() throws Exception {
    FunctionalTestComponent component = getFunctionalTestComponent("AsyncService");
    component.setEventCallback((context, comp, muleContext) -> {
      Thread.sleep(200);
      context.getMessageAsString(muleContext);
    });

    PostMethod request = new PostMethod("http://localhost:" + dynamicPort1.getNumber());
    RequestEntity entity = new StringRequestEntity(MESSAGE, "text/plain", muleContext.getConfiguration().getDefaultEncoding());
    request.setRequestEntity(entity);
    httpClient.executeMethod(request);

    MuleMessage message = muleClient.request("vm://out", 1000).getRight().get();
    assertNotNull(message);
    assertEquals(MESSAGE, getPayloadAsString(message));
  }

  @Test
  public void testAsyncPostWithPersistentSedaQueue() throws Exception {
    FunctionalTestComponent component = getFunctionalTestComponent("AsyncPersistentQueueService");
    component.setEventCallback((context, comp, muleContext) -> {
      Thread.sleep(200);
      context.getMessageAsString(muleContext);
    });

    PostMethod request = new PostMethod("http://localhost:" + dynamicPort2.getNumber());
    RequestEntity entity = new StringRequestEntity(MESSAGE, "text/plain", muleContext.getConfiguration().getDefaultEncoding());
    request.setRequestEntity(entity);

    httpClient.executeMethod(request);
    MuleMessage message = muleClient.request("vm://out", 1000).getRight().get();
    assertNotNull(message);
    assertEquals(MESSAGE, getPayloadAsString(message));
  }
}
