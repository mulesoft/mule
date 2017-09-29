/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;
import static org.mule.test.allure.AllureConstants.XmlSdk.Streaming.STREAMING;

import org.mule.runtime.api.streaming.bytes.CursorStream;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.processor.chain.ModuleOperationMessageProcessorChainBuilder.ModuleOperationProcessorChain;

import org.junit.Test;

import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test case to ensure {@link ModuleOperationProcessorChain#apply(org.reactivestreams.Publisher)}
 * does the correct handle of the chaining when working with events and context child. It also checks if in those cases when
 * an exception happens, {@link ModuleOperationProcessorChain#workOutInternalError(MessagingException, CoreEvent)} does handle
 * the {@link MessagingException} correctly (setting the right {@link CoreEvent} to it).
 * <p/>
 * The idea is ensure all the streams created within a Smart Connector operation are accessible from within a <flow/>, or another
 * Smart Connector.
 */
@Feature(XML_SDK)
@Story(STREAMING)
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
    CoreEvent muleEvent = flowRunner("testHttpDoLoginAndPlainEntireStreamResponseNestingScopes").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, String> resultMap = (Map<String, String>) muleEvent.getMessage().getPayload().getValue();
    assertThat(resultMap.size(), is(3));
    assertThat(resultMap.get("route 0"), is("User and pass validated"));
    assertThat(resultMap.get("route 1").trim(),
               is("index 3:[User and pass validated] index 2:[User and pass validated] index 1:[User and pass validated]"));
    assertThat(resultMap.get("route 2"), is("User and pass validated"));
  }

  @Test
  public void testHttpDoLoginAndPlainEntireStreamResponseNestingScopesWithFailures() throws Exception {
    assertFlow("testHttpDoLoginAndPlainEntireStreamResponseNestingScopesWithFailures", SUCCESS_RESPONSE);
  }

  @Test
  public void testDoLoginFailPropagateErrorDescription() throws Exception {
    CoreEvent muleEvent = flowRunner("testDoLoginFailPropagateErrorDescription").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, String> resultMap = (Map<String, String>) muleEvent.getMessage().getPayload().getValue();
    assertThat(resultMap.size(), is(2));
    assertThat(resultMap.get("errorDescription"), containsString("failed: unauthorized (401)"));
    assertThat(resultMap.get("varsPreError").trim(), is("variable before error"));
  }

  @Test
  public void testDoLoginFailPropagateErrorDescriptionWithinFlow() throws Exception {
    CoreEvent muleEvent = flowRunner("testDoLoginFailPropagateErrorDescriptionWithinFlow").run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), instanceOf(Map.class));
    Map<String, String> resultMap = (Map<String, String>) muleEvent.getMessage().getPayload().getValue();
    assertThat(resultMap.size(), is(2));
    assertThat(resultMap.get("errorDescription"), containsString("failed: unauthorized (401)"));
    assertThat(resultMap.get("varsPreError").trim(), is("variable before error"));
  }

  @Test
  public void testHttpDoLoginAndPlainEntireStreamResponseWithinFlowLeavingStreamingOpen() throws Exception {
    assertFlowWithStreamOpen("testHttpDoLoginAndPlainEntireStreamResponseWithinFlowLeavingStreamingOpen", SUCCESS_RESPONSE);
  }

  @Test
  public void testHttpDoLoginAndPlainBodyStreamResponseWithinFlowLeavingStreamingOpen() throws Exception {
    assertFlowWithStreamOpen("testHttpDoLoginAndPlainBodyStreamResponseWithinFlowLeavingStreamingOpen",
                             USER_AND_PASS_VALIDATED_RESPONSE);
  }

  private void assertFlow(String flowName, String response) throws Exception {
    CoreEvent muleEvent = flowRunner(flowName).run();
    assertThat(muleEvent.getMessage().getPayload().getValue(), is(response));
  }

  private void assertFlowWithStreamOpen(String flowName, String response) throws Exception {
    final CoreEvent muleEvent = flowRunner(flowName).keepStreamsOpen().run();
    final CursorStreamProvider provider = (CursorStreamProvider) muleEvent.getMessage().getPayload().getValue();
    final CursorStream cursorStream = provider.openCursor();
    final String realResult = IOUtils.toString(cursorStream);
    assertThat(realResult, is(response));
  }
}
