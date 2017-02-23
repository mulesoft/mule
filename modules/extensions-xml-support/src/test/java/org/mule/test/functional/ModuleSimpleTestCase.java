/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import org.junit.Test;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;

public class ModuleSimpleTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "module-simple/module-simple.xml";
  }

  @Override
  protected String getConfigFile() {
    return "functional/flows-using-module-simple.xml";
  }

  @Test
  public void testSetPayloadHardcodedFlow() throws Exception {
    Event event = flowRunner("testSetPayloadHardcodedFlow").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hardcoded value"));
  }

  @Test
  public void testSetPayloadParamFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("new payload"));
  }

  @Test
  public void testSetPayloadParamDefaultFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamDefaultFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("15"));
  }

  @Test
  public void testSetPayloadNoSideEffectFlowVariable() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadNoSideEffectFlowVariable").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("10"));
    assertThat(muleEvent.getVariable("testVar").getValue(), is("unchanged value"));
  }

  @Test
  public void testDoNothingFlow() throws Exception {
    Event muleEvent = flowRunner("testDoNothingFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("before calling"));
    assertThat(muleEvent.getVariable("variableBeforeCalling").getValue(), is("value of flowvar before calling"));
  }

  @Test
  public void testSetPayloadParamValueAppender() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamValueAppender").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("new payload from module"));
  }

  @Test
  public void testSetPayloadConcatParamsValues() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadConcatParamsValues").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("105"));
  }

  @Test
  public void testSetPayloadUsingUndefinedParam() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadUsingUndefinedParam").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void testSetPayloadHardcodedFlowWithTarget() throws Exception {
    Event event = flowRunner("testSetPayloadHardcodedFlowWithTarget").run();
    assertThat(event.getMessage().getPayload().getValue(), nullValue());
    final TypedValue<Object> targetVariable = event.getVariable("target-variable");
    assertThat(targetVariable, notNullValue());
    assertThat(targetVariable.getValue(), instanceOf(Message.class));
    Message targetMessage = (Message) targetVariable.getValue();
    assertThat(targetMessage.getPayload().getValue(), is("hardcoded value"));
  }

  @Test
  public void testSetPayloadHardcodedFlowWithTargetOverridingAnExistingVariable() throws Exception {
    Event event = flowRunner("testSetPayloadHardcodedFlowWithTargetOverridingAnExistingVariable").run();
    assertThat(event.getMessage().getPayload().getValue(), nullValue());
    final TypedValue<Object> targetVariable = event.getVariable("existing-variable");
    assertThat(targetVariable, notNullValue());
    assertThat(targetVariable.getValue(), instanceOf(Message.class));
    Message targetMessage = (Message) targetVariable.getValue();
    assertThat(targetMessage.getPayload().getValue(), is("hardcoded value"));
  }
}
