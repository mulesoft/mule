/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import org.junit.Test;

public class FlowTestCase extends AbstractELTestCase {

  public FlowTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  @Override
  public void setupFlowConstruct() throws Exception {
    flowConstruct = getTestFlow("flowName", Object.class);
  }

  @Test
  public void flowName() throws Exception {
    MuleEvent event =
        new DefaultMuleEvent(create(flowConstruct), MuleMessage.builder().payload("").build(), ONE_WAY, flowConstruct);
    assertEquals("flowName", evaluate("flow.name", event));
  }

  @Test
  public void assignToFlowName() throws Exception {
    MuleEvent event =
        new DefaultMuleEvent(create(flowConstruct), MuleMessage.builder().payload("").build(), ONE_WAY, flowConstruct);
    assertFinalProperty("flow.name='foo'", event);
  }

}
