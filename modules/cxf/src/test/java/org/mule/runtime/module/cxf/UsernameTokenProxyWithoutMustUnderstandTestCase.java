/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.cxf.wssec.ClientPasswordCallback;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class UsernameTokenProxyWithoutMustUnderstandTestCase extends FunctionalTestCase {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  @Rule
  public final DynamicPort httpPortProxy = new DynamicPort("port1");

  private String request;
  private String response;

  @Override
  protected String getConfigFile() {
    return "cxf-proxy-service-without-mustunderstand-flow-httpn.xml";
  }

  @Before
  public void doSetUp() throws Exception {
    request = IOUtils.getResourceAsString("in-message-with-mustunderstand.xml", getClass());
    response = IOUtils.getResourceAsString("out-message-with-mustunderstand.xml", getClass());
    ClientPasswordCallback.setPassword("secret");
    super.doSetUp();
    XMLUnit.setIgnoreWhitespace(true);
  }

  @Test
  public void testProxyServiceWithoutMustUnderstand() throws Exception {
    MuleMessage replyMessage = sendRequest("http://localhost:" + httpPortProxy.getNumber() + "/proxy-envelope", request);
    assertNotNull(replyMessage);
    String payload = getPayloadAsString(replyMessage);
    assertFalse(payload.contains("Fault"));
    assertTrue(XMLUnit.compareXML(response, payload).identical());
  }

  protected MuleMessage sendRequest(String url, String payload) throws MuleException {
    return muleContext.getClient().send(url, getTestMuleMessage(payload), HTTP_REQUEST_OPTIONS).getRight();
  }
}
