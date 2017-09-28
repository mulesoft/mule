/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.MuleTestUtils.createFlow;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;

import org.junit.Test;

public class FlowTestCase extends AbstractELTestCase {

  public FlowTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Override
  public void setupFlowConstruct() throws Exception {
    flowConstruct = createFlow(muleContext, "flowName");
  }

  @Test
  public void flowName() throws Exception {
    CoreEvent event = InternalEvent.builder(context).message(of("")).build();
    assertEquals("flowName", evaluate("flow.name", event));
  }

  @Test
  public void assignToFlowName() throws Exception {
    CoreEvent event = InternalEvent.builder(context).message(of("")).build();
    assertFinalProperty("flow.name='foo'", event);
  }

}
