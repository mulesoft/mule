/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import org.mule.runtime.core.api.Event;

import org.hamcrest.core.Is;
import org.junit.Test;

public class ModuleSimpleTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getModulePath() {
    return "module/module-simple/module-simple.xml";
  }

  @Override
  protected String getConfigFile() {
    return "module/flows-using-module-simple.xml";
  }

  @Test
  public void testSetPayloadHardcodedFlow() throws Exception {
    Event event = flowRunner("testSetPayloadHardcodedFlow").run();
    assertThat(event.getMessage().getPayload().getValue(), Is.is("hardcoded value"));
  }

  @Test
  public void testSetPayloadParamFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("new payload"));
  }

  @Test
  public void testSetPayloadParamDefaultFlow() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamDefaultFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("15"));
  }

  @Test
  public void testSetPayloadNoSideEffectFlowVariable() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadNoSideEffectFlowVariable").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("10"));
    assertThat(muleEvent.getVariable("testVar").getValue(), Is.is("unchanged value"));
  }

  @Test
  public void testDoNothingFlow() throws Exception {
    Event muleEvent = flowRunner("testDoNothingFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("before calling"));
    assertThat(muleEvent.getVariable("variableBeforeCalling").getValue(), Is.is("value of flowvar before calling"));
  }

  @Test
  public void testSetPayloadParamValueAppender() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadParamValueAppender").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("new payload from module"));
  }

  @Test
  public void testSetPayloadConcatParamsValues() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadConcatParamsValues").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is("105"));
  }

  @Test
  public void testSetPayloadUsingUndefinedParam() throws Exception {
    Event muleEvent = flowRunner("testSetPayloadUsingUndefinedParam").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), Is.is(nullValue()));
  }
}
