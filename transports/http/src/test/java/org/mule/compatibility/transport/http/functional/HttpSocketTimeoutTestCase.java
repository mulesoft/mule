/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

public class HttpSocketTimeoutTestCase extends CompatibilityFunctionalTestCase {

  private static final int LATCH_TIMEOUT = 300;
  private static final int POLL_TIMEOUT = 600;
  private static final int POLL_DELAY = 150;

  @Rule
  public DynamicPort port = new DynamicPort("port");

  private static Latch latch;
  private static boolean timedOut;

  @Override
  protected String getConfigFile() {
    return "http-socket-timeout-config.xml";
  }

  @Before
  public void setUp() {
    latch = new Latch();
    timedOut = false;
  }

  @Test
  public void usesSoTimeoutIfAvailable() throws Exception {
    Message message = flowRunner("timeout").withPayload(TEST_MESSAGE).run().getMessage();
    assertThat(message, notNullValue());
    assertThat(message.getPayload(), notNullValue());
  }

  @Test
  public void usesResponseTimeoutByDefault() throws Exception {
    flowRunner("noTimeout").withPayload(TEST_MESSAGE).run();
    new PollingProber(POLL_TIMEOUT, POLL_DELAY).check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return timedOut;
      }

      @Override
      public String describeFailure() {
        return "HTTP request should have timed out.";
      }
    });
  }

  protected static class WaitFailureProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      try {
        timedOut = latch.await(LATCH_TIMEOUT, MILLISECONDS);
      } catch (InterruptedException e) {
        // Do nothing
      }
      return event;
    }
  }

  protected static class ReleaseLatchProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      latch.release();
      return event;
    }
  }
}
