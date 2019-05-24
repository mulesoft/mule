/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.policy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class PolicyBackPressureTestCase extends AbstractPolicyTestCase {

  private static Semaphore policyBlockSync = new Semaphore(0);
  private static AtomicInteger messagesInPolicy = new AtomicInteger(0);
  private static AtomicInteger requestStarted = new AtomicInteger(0);
  private static AtomicInteger failedRequests = new AtomicInteger(0);

  @Override
  protected String getConfigFile() {
    return "test-config-backpressure-firing-policy.xml";
  }

  @Test
  public void testSimplePolicy() throws Exception {

    CoreEvent response = flowRunner("testFlow").withPayload("hola").run();
    assertThat(response.getMessage().getPayload().getValue(), is("hola perro"));
    assertThat(response.getVariables().get("sourceState"), notNullValue());
    assertThat(response.getVariables().get("sourceState").getValue(), is("hola"));
  }


  /**
   * Configures a test component that blocks until a specific number of concurrent requests are reached.
   */
  public static class BlocksMP implements Processor
  {

    @Override
    public CoreEvent process(final CoreEvent event) {
      try {
        messagesInPolicy.incrementAndGet();
        policyBlockSync.acquire();
      } catch (Throwable e) {
        // Do nothing.
      }
      return event;
    }
  }

}
