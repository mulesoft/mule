/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ScopeExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  public static final String HEISENBERG = "heisenberg";
  private static final String KILL_REASON = "I'm the one who knocks";
  private static final String GUSTAVO_FRING = "Gustavo Fring";
  private static final String GOODBYE_MESSAGE = "Say hello to my little friend";
  private static final String VICTIM = "Skyler";
  private static final String EMPTY_STRING = "";

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
    expectedException.expectMessage("ON_SUCCESS_EXCEPTION");
    runFlow("exceptionOnCallbacksSuccess");
  }

  @Test
  public void exceptionOnCallbacksFailure() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("ON_ERROR_EXCEPTION");
    runFlow("exceptionOnCallbacksFailure");
  }

  @Test
  @org.junit.Ignore("TODO FOREACH")
  public void manyNestedOperations() throws Exception {
    InternalEvent event = runFlow("killMany");
    String expected = "Killed the following because I'm the one who knocks:\n" + "bye bye, Gustavo Fring\n" + "bye bye, Frank\n"
        + "bye bye, Nazi dudes\n";

    assertThat(event.getMessageAsString(muleContext), is(expected));
  }

  @Test
  @org.junit.Ignore("TODO FOREACH")
  public void manyNestedOperationsSupportedButOnlyOneProvided() throws Exception {
    InternalEvent event = runFlow("killManyButOnlyOneProvided");
    String expected = "Killed the following because I'm the one who knocks:\n" + "bye bye, Gustavo Fring\n";

    assertThat(expected, is(event.getMessageAsString(muleContext)));
  }

  @Test
  public void anything() throws Exception {
    InternalEvent event = flowRunner("executeAnything")
        .withPayload("Killed the following because I'm the one who knocks:").run();
    String expected = "Killed the following because I'm the one who knocks:";

    assertThat(event.getMessage().getPayload().getValue(), is(expected));
  }

  @Test
  public void neverFailsWrapperFailingChain() throws Exception {
    InternalEvent event = flowRunner("neverFailsWrapperFailingChain").run();

    assertThat(event.getMessage().getPayload().getValue(), is("ERROR"));
    assertThat(event.getVariables().get("varName").getValue(), is("varValue"));
  }

  @Test
  public void neverFailsWrapperSuccessChain() throws Exception {
    InternalEvent event = flowRunner("neverFailsWrapperSuccessChain")
        .withVariable("newpayload", "newpayload2")
        .run();

    assertThat(event.getMessage().getPayload().getValue(), is("SUCCESS"));
    assertThat(event.getVariables().get("varName").getValue(), is("varValue"));
  }

  @Test
  public void neverFailsWrapperNoChain() throws Exception {
    InternalEvent event = flowRunner("neverFailsWrapperNoChain").run();

    assertThat(event.getMessage().getPayload().getValue(), is("EMPTY"));
  }

}
