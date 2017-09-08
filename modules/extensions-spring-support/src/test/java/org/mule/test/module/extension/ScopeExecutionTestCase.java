/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ScopeExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String KILL_REASON = "I'm the one who knocks";

  @Rule
  public SystemProperty maxRedelivery = new SystemProperty("killingReason", KILL_REASON);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"heisenberg-scope-config.xml"};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Test
  public void verifyProcessorInitialise() throws Exception {
    runFlow("getChain").getMessage().getPayload().getValue();
    runFlow("getChain").getMessage().getPayload().getValue();
    runFlow("getChain").getMessage().getPayload().getValue();
    int value = (int) runFlow("getCounter").getMessage().getPayload().getValue();
    assertThat(value, is(1));
  }

  @Test
  public void verifySameProcessorInstance() throws Exception {
    Object getChainFirst = runFlow("getChain").getMessage().getPayload().getValue();
    Object getChainSecond = runFlow("getChain").getMessage().getPayload().getValue();
    assertThat(getChainFirst, is(not(sameInstance(getChainSecond))));

    Object firstChain = ClassUtils.getFieldValue(getChainFirst, "chain", false);
    Object secondChain = ClassUtils.getFieldValue(getChainSecond, "chain", false);
    assertThat(firstChain, is(sameInstance(secondChain)));
  }

  @Test
  public void alwaysFailsWrapperFailure() throws Exception {
    expectedException.expect(instanceOf(MessagingException.class));
    // Exceptions are converted in the extension's exception enricher
    expectedException.expectCause(instanceOf(ConnectionException.class));
    expectedException.expectMessage("ON_ERROR_ERROR");
    runFlow("alwaysFailsWrapperFailure");
  }

  @Test
  public void alwaysFailsWrapperSuccess() throws Exception {
    expectedException.expect(instanceOf(MessagingException.class));
    // Exceptions are converted in the extension's exception enricher
    expectedException.expectCause(instanceOf(ConnectionException.class));
    expectedException.expectMessage("ON_SUCCESS_ERROR");
    runFlow("alwaysFailsWrapperSuccess");
  }

  @Test
  public void exceptionOnCallbacksSuccess() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    // When an exception occurs in the "onSuccess", we then invoke the onError
    expectedException.expectMessage("ON_ERROR_EXCEPTION");
    runFlow("exceptionOnCallbacksSuccess");
  }

  @Test
  public void exceptionOnCallbacksFailure() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("ON_ERROR_EXCEPTION");
    runFlow("exceptionOnCallbacksFailure");
  }

  @Test
  @Ignore("MULE-13440")
  public void manyNestedOperations() throws Exception {
    BaseEvent event = runFlow("killMany");
    String expected = "Killed the following because I'm the one who knocks:\n" + "bye bye, Gustavo Fring\n" + "bye bye, Frank\n"
        + "bye bye, Nazi dudes\n";

    assertThat(((PrivilegedEvent) event).getMessageAsString(muleContext), is(expected));
  }

  @Test
  @Ignore("MULE-13440")
  public void manyNestedOperationsSupportedButOnlyOneProvided() throws Exception {
    BaseEvent event = runFlow("killManyButOnlyOneProvided");
    String expected = "Killed the following because I'm the one who knocks:\n" + "bye bye, Gustavo Fring\n";

    assertThat(expected, is(((PrivilegedEvent) event).getMessageAsString(muleContext)));
  }

  @Test
  public void anything() throws Exception {
    BaseEvent event = flowRunner("executeAnything")
        .withPayload("Killed the following because I'm the one who knocks:").run();
    String expected = "Killed the following because I'm the one who knocks:";

    assertThat(event.getMessage().getPayload().getValue(), is(expected));
  }

  @Test
  public void neverFailsWrapperFailingChain() throws Exception {
    BaseEvent event = flowRunner("neverFailsWrapperFailingChain").run();

    assertThat(event.getMessage().getPayload().getValue(), is("ERROR"));
    assertThat(event.getVariables().get("varName").getValue(), is("varValue"));
  }

  @Test
  public void neverFailsWrapperSuccessChain() throws Exception {
    BaseEvent event = flowRunner("neverFailsWrapperSuccessChain")
        .withVariable("newpayload", "newpayload2")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("SUCCESS"));
    assertThat(event.getVariables().get("varName").getValue(), is("varValue"));
  }

  @Test
  public void payloadModifier() throws Exception {
    BaseEvent event = flowRunner("payloadModifier").run();

    assertThat(event.getMessage().getPayload().getValue(), is("MESSAGE"));
    assertThat(event.getVariables().get("newPayload").getValue(), is("MESSAGE"));
    assertThat(event.getVariables().get("newAttributes").getValue(), is(notNullValue()));
  }

  @Test
  public void neverFailsWrapperNoChain() throws Exception {
    BaseEvent event = flowRunner("neverFailsWrapperNoChain").run();

    assertThat(event.getMessage().getPayload().getValue(), is("EMPTY"));
  }

}
