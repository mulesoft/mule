/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalStreamingTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;

/**
 * This test is more about testing the streaming model than the TCP provider, really.
 */
public class StreamingTestCase extends FunctionalTestCase {

  public static final int TIMEOUT = 300000;
  public static final String TEST_MESSAGE = "Test TCP Request";
  public static final String RESULT = "Received stream; length: 16; 'Test...uest'";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Override
  protected String getConfigFile() {
    return "tcp-streaming-test-flow.xml";
  }

  @Test
  public void testSend() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<String> message = new AtomicReference<String>();
    final AtomicInteger loopCount = new AtomicInteger(0);

    EventCallback callback = new EventCallback() {

      @Override
      public synchronized void eventReceived(MuleEventContext context, Object component) {
        try {
          logger.info("called " + loopCount.incrementAndGet() + " times");
          FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
          // without this we may have problems with the many repeats
          if (1 == latch.getCount()) {
            message.set(ftc.getSummary());
            assertEquals(RESULT, message.get());
            latch.countDown();
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    };

    MuleClient client = muleContext.getClient();

    // this works only if singleton set in descriptor
    Object ftc = getComponent("testComponent");
    assertTrue("FunctionalStreamingTestComponent expected", ftc instanceof FunctionalStreamingTestComponent);
    assertNotNull(ftc);

    ((FunctionalStreamingTestComponent) ftc).setEventCallback(callback, TEST_MESSAGE.length());

    client.dispatch(((InboundEndpoint) ((Flow) muleContext.getRegistry().lookupObject("testComponent")).getMessageSource())
        .getAddress(), TEST_MESSAGE, new HashMap());

    latch.await(10, TimeUnit.SECONDS);
    assertEquals(RESULT, message.get());
  }
}
