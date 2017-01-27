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
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import static org.mule.service.http.api.HttpConstants.Methods.POST;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class UsernameTokenProxyTestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

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
    InternalMessage result = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-envelope");
    assertFalse(getPayloadAsString(result).contains("Fault"));
    assertTrue(getPayloadAsString(result).contains("joe"));
  }

  @Ignore("MULE-6926: Flaky Test")
  @Test
  public void testProxyBody() throws Exception {
    InternalMessage result = sendRequest("http://localhost:" + dynamicPort.getNumber() + "/proxy-body");

    assertFalse(getPayloadAsString(result).contains("Fault"));
    assertFalse(getPayloadAsString(result).contains("joe"));
  }

  protected InternalMessage sendRequest(String url) throws MuleException {
    InputStream stream = getClass().getResourceAsStream(getMessageResource());
    assertNotNull(stream);

    return muleContext.getClient().send(url, InternalMessage.builder().payload(stream).build(), HTTP_REQUEST_OPTIONS).getRight();
  }

  protected String getMessageResource() {
    return "/org/mule/compatibility/module/cxf/wssec/in-message.xml";
  }
}
