/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder;

import java.util.Map;

/**
 * Test case to ensure {@link ModuleOperationMessageProcessorChainBuilder.ModuleOperationProcessorChain#apply(org.reactivestreams.Publisher)}
 * does the correct handle of the chaining when working with events and context child.
 * <p/>
 * The idea is ensure all the streams created within a Smart Connector operation are accessible from within a <flow/>, or another
 * Smart Connector.
 */
public class ModuleWithStreamOperationTestCase extends AbstractModuleWithHttpTestCase {

  @Override
  protected String[] getModulePaths() {
    return new String[] {"modules/module-using-streaming.xml"};
  }

  @Override
  protected String getConfigFile() {
    return "flows/flows-using-module-using-streaming.xml";
  }

  @Test
  public void testHttpDoLoginAndPlainEntireStreamResponseWithinOperation() throws Exception {
    assertFlow("testHttpDoLoginAndPlainEntireStreamResponseWithinOperation", SUCCESS_RESPONSE);
  }

  @Test
  public void testHttpDoLoginAndPlainEntireStreamResponseWithinFlow() throws Exception {
    assertFlow("testHttpDoLoginAndPlainEntireStreamResponseWithinFlow", SUCCESS_RESPONSE);
  }

  @Test
  public void testHttpDoLoginAndPlainBodyStreamResponseWithinOperation() throws Exception {
    assertFlow("testHttpDoLoginAndPlainBodyStreamResponseWithinOperation", USER_AND_PASS_VALIDATED_RESPONSE);
  }

  @Test
  public void testHttpDoLoginAndPlainBodyStreamResponseWithinFlow() throws Exception {
    assertFlow("testHttpDoLoginAndPlainBodyStreamResponseWithinFlow", USER_AND_PASS_VALIDATED_RESPONSE);
  }

  @Test
  public void testHttpDoLoginAndPlainEntireStreamResponseWithinOperationTns() throws Exception {
    assertFlow("testHttpDoLoginAndPlainEntireStreamResponseWithinOperationTns", SUCCESS_RESPONSE);
  }

  @Test
  public void testHttpDoLoginAndPlainEntireStreamResponseNestingScopes() throws Exception {
    BaseEvent muleEvent = flowRunner("testHttpDoLoginAndPlainEntireStreamResponseNestingScopes").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, String> resultMap = (Map<String, String>) muleEvent.getMessage().getPayload().getValue();
    assertThat(resultMap.size(), is(3));
    assertThat(resultMap.get("route 0"), is("User and pass validated"));
    assertThat(resultMap.get("route 1").trim(),
               is("index 3:[User and pass validated] index 2:[User and pass validated] index 1:[User and pass validated]"));
    assertThat(resultMap.get("route 2"), is("User and pass validated"));
  }

  private void assertFlow(String flowName, String response) throws Exception {
    BaseEvent muleEvent = flowRunner(flowName).run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(response));
  }
}
