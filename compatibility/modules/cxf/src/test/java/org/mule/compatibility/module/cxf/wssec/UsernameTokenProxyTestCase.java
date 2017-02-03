/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.wssec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Method.POST;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class UsernameTokenProxyTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder().build();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"org/mule/compatibility/module/cxf/wssec/username-token-conf.xml"};
  }

  @Override
  protected void doSetUp() throws Exception {
    ClientPasswordCallback.setPassword("secret");
    super.doSetUp();
  }

  @Ignore("MULE-6926: Flaky Test")
  @Test
  public void testProxyEnvelope() throws Exception {
    HttpResponse httpResponse = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-envelope");

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertFalse(payload.contains("Fault"));
    assertTrue(payload.contains("joe"));
  }

  @Ignore("MULE-6926: Flaky Test")
  @Test
  public void testProxyBody() throws Exception {
    HttpResponse httpResponse = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-body");

    String payload = IOUtils.toString(((InputStreamHttpEntity) httpResponse.getEntity()).getInputStream());
    assertFalse(payload.contains("Fault"));
    assertFalse(payload.contains("joe"));
  }

  protected HttpResponse sendRequest(String url) throws MuleException, IOException, TimeoutException {
    InputStream stream = getClass().getResourceAsStream(getMessageResource());
    assertNotNull(stream);

    HttpRequest httpRequest = HttpRequest.builder().setUri(url).setEntity(new InputStreamHttpEntity(stream))
        .setMethod(POST).build();

    return httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

  }

  protected String getMessageResource() {
    return "/org/mule/compatibility/module/cxf/wssec/in-message.xml";
  }
}
