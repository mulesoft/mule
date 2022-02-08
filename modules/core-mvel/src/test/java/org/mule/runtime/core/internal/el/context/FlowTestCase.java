/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.tck.MuleTestUtils.createFlow;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.After;
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

  @After
  public void after() {
    disposeIfNeeded(flowConstruct, getLogger(getClass()));
  }

  @Test
  public void flowName() {
    CoreEvent event = InternalEvent.builder(context).message(of("")).build();
    assertThat(evaluate("flow.name", event), equalTo("flowName"));
  }

  @Test
  public void assignToFlowName() {
    CoreEvent event = InternalEvent.builder(context).message(of("")).build();
    assertFinalProperty("flow.name='foo'", event);
  }

}
