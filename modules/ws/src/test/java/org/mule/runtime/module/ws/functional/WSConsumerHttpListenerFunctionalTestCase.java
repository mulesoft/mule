/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.ws.functional;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

/**
 * This test case verifies that an HTTP listener is able to return the response of a WS consumer when no transformation is
 * performed.
 */
public class WSConsumerHttpListenerFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase {

  @Rule
  public DynamicPort clientPort = new DynamicPort("clientPort");


  @Override
  protected String getConfigFile() {
    return "ws-consumer-http-listener-config.xml";
  }

  @Test
  public void listenerReturnsSoapEnvelopeXMLCorrectly() throws Exception {
    MuleClient client = muleContext.getClient();
    MuleMessage response = client.send("http://localhost:" + clientPort.getValue(), getTestMuleMessage(ECHO_REQUEST),
                                       newOptions().method(POST.name()).build())
        .getRight();
    assertXMLEqual(EXPECTED_ECHO_RESPONSE, getPayloadAsString(response));
  }

}
