/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RoutersExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String KILL_REASON = "I'm the one who knocks";

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty("killingReason", KILL_REASON);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-router-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void singleRouteRouter() throws Exception {
    BaseEvent internalEvent = flowRunner("singleRouteRouter").withPayload("message").withAttributes("other").run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("message"));
    assertThat(internalEvent.getVariables().get("newPayload").getValue(), is("message"));
    assertThat(internalEvent.getVariables().get("newAttributes").getValue(), is("other"));
  }

  @Test
  public void twoRoutesRouterWhen() throws Exception {
    BaseEvent internalEvent = flowRunner("twoRoutesRouter")
        .withVariable("executeWhen", true)
        .withVariable("executeOther", false).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload").getValue(), is("mule:set-payload"));
  }

  @Test
  public void twoRoutesRouterOther() throws Exception {
    BaseEvent internalEvent = flowRunner("twoRoutesRouter")
        .withVariable("executeWhen", false)
        .withVariable("executeOther", true).run();

    assertThat(internalEvent.getMessage().getPayload().getValue(), is("mule:set-payload"));
    assertThat(internalEvent.getVariables().get("newPayload"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("newAttributes"), is(nullValue()));
  }

  @Test
  public void twoRoutesRouterNone() throws Exception {
    expectedException.expectCause(instanceOf(ConnectionException.class));
    expectedException.expectMessage("No route executed");
    runFlow("twoRoutesRouterNone");
  }

  @Test
  public void munitSpy() throws Exception {
    BaseEvent internalEvent = flowRunner("munitSpy").run();
    assertThat(internalEvent.getVariables().get("before").getValue(), is("true"));
    assertThat(internalEvent.getVariables().get("after").getValue(), is("true"));
  }

  @Test
  public void munitSpyNoBefore() throws Exception {
    BaseEvent internalEvent = flowRunner("munitSpyNoBefore").run();
    assertThat(internalEvent.getMessage().getPayload().getValue(), is("1"));
    assertThat(internalEvent.getVariables().get("before"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("after").getValue(), is("true"));
  }

  @Test
  public void munitSpyNoAfter() throws Exception {
    BaseEvent internalEvent = flowRunner("munitSpyNoAfter").run();
    assertThat(internalEvent.getMessage().getPayload().getValue(), is("2"));
    assertThat(internalEvent.getVariables().get("before").getValue(), is("true"));
    assertThat(internalEvent.getVariables().get("after"), is(nullValue()));
  }

  @Test
  public void munitSpyNoAttributes() throws Exception {
    BaseEvent internalEvent = flowRunner("munitSpyNoAttributes").run();
    assertThat(internalEvent.getMessage().getPayload().getValue(), is(nullValue()));
    assertThat(internalEvent.getVariables().get("before"), is(nullValue()));
    assertThat(internalEvent.getVariables().get("after"), is(nullValue()));
  }
}
