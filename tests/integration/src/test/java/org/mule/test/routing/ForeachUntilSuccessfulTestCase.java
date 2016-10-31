/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.Event;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class ForeachUntilSuccessfulTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "for-each-until-config.xml";
  }

  @Test
  public void flowVariableSyncNoForeach() throws Exception {
    Event event = runAndAssert("flowVarSyncNoForEach", 3);
    assertThat(event.getVariable("until").getValue(), is(3));
  }

  @Test
  public void flowVariablesSyncArePropagated() throws Exception {
    Event event = runAndAssert("flowVarSync");
    assertThat(event.getVariable("until").getValue(), is(3));
  }

  @Test
  public void flowVariablesAsyncArePropagated() throws Exception {
    runAndAssert("flowVarAsync");
  }

  private Event runAndAssert(String flowName) throws Exception {
    return runAndAssert(flowName, newArrayList(1, 2, 3));
  }

  private Event runAndAssert(String flowName, Object payload) throws Exception {
    Event event = flowRunner(flowName).withPayload(payload).run();
    assertThat(event.getVariable("count").getValue(), is(6));
    assertThat(event.getVariable("current").getValue(), is(3));
    return event;
  }
}
