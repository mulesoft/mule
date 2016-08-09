/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalStreamingTestComponent;
import org.mule.runtime.core.api.MuleEventContext;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpStreamingTestCase extends SocketExtensionTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(TcpStreamingTestCase.class);
  public static final String TEST_MESSAGE = "Test TCP Request";
  public static final String RESULT = "Received stream; length: %d; 'Test...uest'";
  private static int SINGLE_ITERATION = 1;

  @Override
  protected String getConfigFile() {
    return "streaming-protocol-config.xml";
  }

  @Test
  public void testStreamingWithInputStreamPayload() throws Exception {
    streamingTestCase(new ByteArrayInputStream(TEST_MESSAGE.getBytes()), SINGLE_ITERATION);
  }

  @Test
  public void testStreamingWithStringPayload() throws Exception {
    streamingTestCase(TEST_MESSAGE, SINGLE_ITERATION);
  }

  /**
   * @param payload to be sent in each request
   * @param iterations amount of times that the send flow should be called
   * @throws Exception
   */
  private void streamingTestCase(Object payload, int iterations) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<String> message = new AtomicReference<String>();
    final AtomicInteger loopCount = new AtomicInteger(0);

    EventCallback callback = new EventCallback() {

      @Override
      public synchronized void eventReceived(MuleEventContext context, Object component) {
        try {
          LOGGER.info(format("called %d times", loopCount.incrementAndGet()));
          FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
          // without this we may have problems with the many repeats
          if (1 == latch.getCount()) {
            message.set(ftc.getSummary());
            assertThat(format(RESULT, TEST_MESSAGE.length()), is(message.get()));
            latch.countDown();
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    };

    // this works only if singleton set in descriptor
    Object functionalTestComponent = getComponent("tcp-listen");
    assertThat(functionalTestComponent, is(instanceOf(FunctionalStreamingTestComponent.class)));
    assertThat(functionalTestComponent, is(notNullValue()));

    ((FunctionalStreamingTestComponent) functionalTestComponent).setEventCallback(callback, TEST_MESSAGE.length());

    flowRunner("tcp-send").withPayload(payload).run().getMessage().getPayload();

    latch.await(10, TimeUnit.SECONDS);
    assertThat(format(RESULT, iterations * TEST_MESSAGE.length()), is(message.get()));
  }
}
