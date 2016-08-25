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

import org.mule.compatibility.transport.http.HttpsConnector;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;

public class HttpsFunctionalTestCase extends HttpFunctionalTestCase {

  public static final String SERVER_KEYSTORE_PATH = "serverKeystorePath";
  public static final String SERVER_KEYSTORE = "serverKeystore";

  @Rule
  public SystemProperty serverKeystoreProperty = new SystemProperty(SERVER_KEYSTORE_PATH, SERVER_KEYSTORE);


  @Override
  protected String getConfigFile() {
    return "https-functional-test-flow.xml";
  }

  @Override
  public void testSend() throws Exception {
    FlowConstruct testSedaService = muleContext.getRegistry().lookupFlowConstruct("testComponent");
    FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(testSedaService);

    assertNotNull(testComponent);

    final AtomicBoolean callbackMade = new AtomicBoolean(false);
    EventCallback callback = (context, component, muleContext) -> {
      MuleMessage msg = context.getMessage();
      assertTrue(callbackMade.compareAndSet(false, true));
      assertNotNull(msg.getOutboundProperty(HttpsConnector.LOCAL_CERTIFICATES));
    };
    testComponent.setEventCallback(callback);

    MuleClient client = muleContext.getClient();

    MuleMessage result =
        client.send("clientEndpoint",
                    MuleMessage.builder().payload(TEST_MESSAGE).mediaType(MediaType.parse("text/plain;charset=UTF-8")).build())
            .getRight();

    assertNotNull(result);
    assertEquals(TEST_MESSAGE + " Received", getPayloadAsString(result));
    assertTrue("Callback never fired", callbackMade.get());
  }
}
