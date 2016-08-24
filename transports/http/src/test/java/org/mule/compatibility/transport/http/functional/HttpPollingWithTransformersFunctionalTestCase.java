/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.FunctionalTestNotificationListener;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;

public class HttpPollingWithTransformersFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "mule-http-polling-with-transformers-config-flow.xml";
  }

  @Test
  public void testPollingHttpConnector() throws Exception {
    final Latch latch = new Latch();
    final AtomicBoolean transformPropagated = new AtomicBoolean(false);
    muleContext.registerListener(new FunctionalTestNotificationListener() {

      @Override
      public void onNotification(ServerNotification notification) {
        latch.countDown();
        if (notification.getSource().toString().endsWith("toClient-only")) {
          transformPropagated.set(true);
        }
      }
    }, "polledUMO");

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.request("vm://toclient", 50000).getRight().get();
    assertNotNull(result.getPayload());
    assertTrue("Callback called", latch.await(1000, TimeUnit.MILLISECONDS));
    assertEquals("/foo toClient-only", getPayloadAsString(result));
    // The transform should not have been propagated to the outbound endpoint
    assertFalse(transformPropagated.get());
  }
}
