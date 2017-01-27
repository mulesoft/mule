/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.service.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WireTapCxfTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/wire-tap-cxf-flow.xml";
  }

  @Test
  public void testWireTap() throws Exception {
    String url = "http://localhost:" + port1.getNumber() + "/services/EchoUMO";
    String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
        + "<soap:Body><echo><text>foo</text></echo></soap:Body></soap:Envelope>";

    MuleClient client = muleContext.getClient();
    InternalMessage response = client.send(url, InternalMessage.of(msg), newOptions().method(POST.name()).build()).getRight();
    assertThat(response, not(nullValue()));

    String responseString = getPayloadAsString(response);
    assertThat(responseString, containsString("echoResponse"));
    assertThat(responseString, not(containsString("soap:Fault")));

    assertThat(client.request("test://wireTapped", RECEIVE_TIMEOUT).getRight().isPresent(), is(true));
  }
}
