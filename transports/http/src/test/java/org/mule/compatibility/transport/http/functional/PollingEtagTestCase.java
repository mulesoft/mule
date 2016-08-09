/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.functional.functional.CounterCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class PollingEtagTestCase extends FunctionalTestCase {

  private static final int WAIT_TIME = 2500;

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "polling-etag-test-flow.xml";
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testPollingReceiversRestart() throws Exception {
    Object ftc = getComponent("Test");
    assertTrue("FunctionalTestComponent expected", ftc instanceof FunctionalTestComponent);

    AtomicInteger pollCounter = new AtomicInteger(0);
    ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

    // should be enough to poll for multiple messages
    Thread.sleep(WAIT_TIME);

    assertEquals(1, pollCounter.get());
  }
}

