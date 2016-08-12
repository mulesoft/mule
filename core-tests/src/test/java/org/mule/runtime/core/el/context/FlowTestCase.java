/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.DefaultMessageExecutionContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.junit.Test;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;

public class FlowTestCase extends AbstractELTestCase {

  public FlowTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  @Test
  public void flowName() throws Exception {
    Flow flow = getTestFlow("flowName", Object.class);
    MuleEvent event =
        new DefaultMuleEvent(create(flow), MuleMessage.builder().payload("").build(), ONE_WAY, flow);
    assertEquals("flowName", evaluate("flow.name", event));
  }

  @Test
  public void assignToFlowName() throws Exception {
    Flow flow = getTestFlow("flowName", Object.class);
    MuleEvent event =
        new DefaultMuleEvent(create(flow), MuleMessage.builder().payload("").build(), ONE_WAY, flow);
    assertFinalProperty("flow.name='foo'", event);
  }

}
