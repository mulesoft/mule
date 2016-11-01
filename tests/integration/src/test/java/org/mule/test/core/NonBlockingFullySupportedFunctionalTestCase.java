/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.ProcessingStrategyFactory;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class NonBlockingFullySupportedFunctionalTestCase extends AbstractIntegrationTestCase {

  public static String FOO = "foo";
  private ProcessingStrategyFactory processingStrategyFactory;

  @Override
  protected String getConfigFile() {
    return "non-blocking-fully-supported-test-config.xml";
  }

  public NonBlockingFullySupportedFunctionalTestCase(ProcessingStrategyFactory processingStrategyFactory) {
    this.processingStrategyFactory = processingStrategyFactory;
  }

  @Parameters
  public static Collection<Object[]> parameters() {
    return Arrays
        .asList(new Object[][] {{new DefaultFlowProcessingStrategyFactory()}, {new NonBlockingProcessingStrategyFactory()}});
  }

  @Override
  protected void configureMuleContext(MuleContextBuilder contextBuilder) {
    DefaultMuleConfiguration configuration = new DefaultMuleConfiguration();
    configuration.setDefaultProcessingStrategyFactory(processingStrategyFactory);
    contextBuilder.setMuleConfiguration(configuration);
  }

  @Test
  public void flow() throws Exception {
    flowRunner("flow").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void subFlow() throws Exception {
    flowRunner("subFlow").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void childFlow() throws Exception {
    flowRunner("childFlow").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void childSyncFlow() throws Exception {
    flowRunner("childSyncFlow").withPayload(TEST_MESSAGE).nonBlocking().withExchangePattern(getMessageExchnagePattern()).run();
    verify("childSyncFlowChild");
  }

  public void childAsyncFlow() throws Exception {
    flowRunner("childAsyncFlow").withPayload(TEST_MESSAGE).nonBlocking().withExchangePattern(getMessageExchnagePattern()).run();
    verify("childAsyncFlowChild");
  }

  @Test
  public void processorChain() throws Exception {
    flowRunner("processorChain").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void filterAccepts() throws Exception {
    flowRunner("filterAccepts").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void filterRejects() throws Exception {
    Event result = flowRunner("filterRejects").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern())
        .nonBlocking().run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void filterAfterNonBlockingAccepts() throws Exception {
    flowRunner("filterAfterNonBlockingAccepts").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern())
        .nonBlocking().run();
  }

  @Test
  public void filterAfterNonBlockingRejects() throws Exception {
    Event result = flowRunner("filterAfterNonBlockingRejects").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void filterBeforeNonBlockingAccepts() throws Exception {
    flowRunner("filterAfterNonBlockingAccepts").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern())
        .nonBlocking().run();
  }

  @Test
  public void filterBeforeNonBlockingRejects() throws Exception {
    Event result = flowRunner("filterAfterNonBlockingRejects").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void filterAfterEnricherBeforeNonBlocking() throws Exception {
    Event result = flowRunner("filterAfterEnricherBeforeNonBlocking").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void securityFilter() throws Exception {
    flowRunner("security-filter").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void transformer() throws Exception {
    flowRunner("transformer").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void choice() throws Exception {
    flowRunner("choice").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void enricher() throws Exception {
    flowRunner("enricher").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void response() throws Exception {
    flowRunner("response").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  public void responseWithNullEvent() throws Exception {
    Event result = flowRunner("responseWithNullEvent").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    assertThat(result, is(nullValue()));
  }

  @Test
  public void enricherIssue() throws Exception {
    Event result = flowRunner("enricherIssue").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern())
        .nonBlocking().run();
    assertThat(result.getMessageAsString(muleContext), is(equalTo(TEST_MESSAGE)));
  }

  @Test
  public void enricherIssueNonBlocking() throws Exception {
    Event result = flowRunner("enricherIssueNonBlocking").withPayload(TEST_MESSAGE)
        .withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
    assertThat(result.getMessageAsString(muleContext), is(equalTo(TEST_MESSAGE)));
  }

  @Test
  public void enricherFlowVar() throws Exception {
    Event result = flowRunner("enricherFlowVar").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern())
        .nonBlocking().run();
    assertThat(result.getVariable(FOO).getValue(), is(equalTo(TEST_MESSAGE)));
  }

  @Test
  public void async() throws Exception {
    flowRunner("async").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking().run();
  }

  @Test
  @Ignore("MULE-10617")
  public void catchExceptionStrategy() throws Exception {
    flowRunner("catchExceptionStrategy").withPayload(TEST_MESSAGE).withExchangePattern(getMessageExchnagePattern()).nonBlocking()
        .run();
    verify("catchExceptionStrategyChild");
  }

  protected MessageExchangePattern getMessageExchnagePattern() {
    return MessageExchangePattern.REQUEST_RESPONSE;
  }
}

