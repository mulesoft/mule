/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.compatibility.core.api.transport.MessageReceiver;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.construct.Flow;

import org.junit.Test;

public class FlowStateEndpointTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/flow-endpoint-initial-state.xml";
  }

  @Test
  public void testDefaultInitialstate() throws Exception {
    doTestStarted("default");
  }

  @Test
  public void testStartedInitialstate() throws Exception {
    doTestStarted("started");
  }

  protected void doTestStarted(String flowName) throws Exception {
    Flow f = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName + "Flow");
    // Flow initially started
    assertTrue(f.getLifecycleState().isStarted());
    assertFalse(f.getLifecycleState().isStopped());

    // The listeners should be registered and started.
    doListenerTests(flowName, 1, true);
  }

  @Test
  public void testInitialStateStopped() throws Exception {
    Flow f = (Flow) muleContext.getRegistry().lookupFlowConstruct("stoppedFlow");
    // Flow initially stopped
    assertFalse(f.getLifecycleState().isStarted());
    assertTrue(f.getLifecycleState().isStopped());

    // The connector should be started, but with no listeners registered.
    doListenerTests("stopped", 0, true);

    f.start();
    assertTrue(f.getLifecycleState().isStarted());
    assertFalse(f.getLifecycleState().isStopped());

    // The listeners should now be registered and started.
    doListenerTests("stopped", 1, true);
  }

  protected void doListenerTests(String receiverName, int expectedCount, boolean isConnected) {
    AbstractConnector connector = (AbstractConnector) muleContext.getRegistry().lookupObject("connector.test.mule.default");
    assertNotNull(connector);
    assertTrue(connector.isStarted());
    MessageReceiver[] receivers = connector.getReceivers("*" + receiverName + "*");
    assertEquals(expectedCount, receivers.length);
    for (int i = 0; i < expectedCount; i++) {
      assertEquals(isConnected, receivers[0].isConnected());
    }
  }

}
