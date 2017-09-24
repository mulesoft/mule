/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ModuleSimpleTestCase extends AbstractXmlExtensionMuleArtifactFunctionalTestCase {

  @Parameterized.Parameter
  public String configFile;

  @Parameterized.Parameter(1)
  public String[] paths;

  @Parameterized.Parameters(name = "{index}: Running tests for {0} ")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // simple scenario
        {"flows/flows-using-module-simple.xml", new String[] {"modules/module-simple.xml"}},
        // nested modules scenario
        {"flows/nested/flows-using-module-simple-proxy.xml",
            new String[] {"modules/module-simple.xml", "modules/nested/module-simple-proxy.xml"}}
    });
  }

  @Override
  protected String[] getModulePaths() {
    return paths;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void testSetPayloadHardcodedFlow() throws Exception {
    CoreEvent event = flowRunner("testSetPayloadHardcodedFlow").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hardcoded value"));
  }

  @Test
  public void testSetPayloadParamFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadParamFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("new payload"));
  }

  @Test
  public void testSetPayloadParamDefaultFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadParamDefaultFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("15"));
  }

  @Test
  public void testSetPayloadParamDefaultUseOptionalFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadParamDefaultUseOptionalFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("15"));
  }

  @Test
  public void testSetPayloadNoSideEffectFlowVariable() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadNoSideEffectFlowVariable").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("10"));
    assertThat(muleEvent.getVariables().get("testVar").getValue(), is("unchanged value"));
  }

  @Test
  public void testDoNothingFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testDoNothingFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("before calling"));
    assertThat(muleEvent.getVariables().get("variableBeforeCalling").getValue(), is("value of flowvar before calling"));
  }

  @Test
  public void testSetPayloadParamValueAppender() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadParamValueAppender").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("new payload from module"));
  }

  @Test
  public void testSetPayloadConcatParamsValues() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadConcatParamsValues").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is("105"));
  }

  @Test
  public void testSetPayloadUsingUndefinedParam() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadUsingUndefinedParam").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void testSetPayloadHardcodedFlowWithTarget() throws Exception {
    CoreEvent event = flowRunner("testSetPayloadHardcodedFlowWithTarget").run();
    assertThat(event.getMessage().getPayload().getValue(), nullValue());
    final TypedValue<?> targetVariable = event.getVariables().get("target-variable");
    assertThat(targetVariable, notNullValue());
    assertThat(targetVariable.getValue(), instanceOf(Message.class));
    Message targetMessage = (Message) targetVariable.getValue();
    assertThat(targetMessage.getPayload().getValue(), is("hardcoded value"));
  }

  @Test
  public void testSetPayloadHardcodedFlowWithTargetOverridingAnExistingVariable() throws Exception {
    CoreEvent event = flowRunner("testSetPayloadHardcodedFlowWithTargetOverridingAnExistingVariable").run();
    assertThat(event.getMessage().getPayload().getValue(), nullValue());
    final TypedValue<?> targetVariable = event.getVariables().get("existing-variable");
    assertThat(targetVariable, notNullValue());
    assertThat(targetVariable.getValue(), instanceOf(Message.class));
    Message targetMessage = (Message) targetVariable.getValue();
    assertThat(targetMessage.getPayload().getValue(), is("hardcoded value"));
  }

  @Test
  public void testSetPayloadHardcodedFlowWithTargetAndTargetValuePayload() throws Exception {
    CoreEvent event = flowRunner("testSetPayloadHardcodedFlowWithTargetAndTargetValuePayload").run();
    assertThat(event.getMessage().getPayload().getValue(), nullValue());
    final TypedValue<?> targetVariable = event.getVariables().get("existing-variable");
    assertThat(targetVariable, notNullValue());
    assertThat(targetVariable.getValue(), instanceOf(String.class));
    String targetMessage = (String) targetVariable.getValue();
    assertThat(targetMessage, is("hardcoded value"));
  }

  @Test
  public void testSetPayloadUsingOptionalParam() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadUsingOptionalParam").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(nullValue()));
  }

  @Test
  public void testSetPayloadUsingParamValueMoreThanOnceFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testSetPayloadUsingParamValueMoreThanOnceFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(),
               is("a payload written 2 or more times in the same operation using the same parameter"));
  }
}
