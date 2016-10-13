/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Ignore;
import org.junit.Test;

public class NonBlockingFullySupportedOneWayFunctionalTestCase extends AbstractIntegrationTestCase {

  public static String FOO = "foo";

  @Override
  protected String getConfigFile() {
    return "non-blocking-fully-supported-oneway-test-config.xml";
  }

  @Test
  @Ignore("MULE-9731")
  public void flow() throws Exception {
    assertVoidMuleEventResponse("flow");
  }

  @Test
  @Ignore("MULE-9731")
  public void subFlow() throws Exception {
    assertVoidMuleEventResponse("subFlow");
  }

  @Test
  @Ignore("MULE-9731")
  public void childFlow() throws Exception {
    assertVoidMuleEventResponse("childFlow");
    verify("childFlowChild");
  }

  @Test
  @Ignore("MULE-9731")
  public void childDefaultFlow() throws Exception {
    flowRunner("childDefaultFlow").withPayload(TEST_MESSAGE).asynchronously().run();
    verify("childDefaultFlowChild");
  }

  @Test
  @Ignore("MULE-9731")
  public void childSyncFlow() throws Exception {
    flowRunner("childSyncFlow").withPayload(TEST_MESSAGE).asynchronously().run();
    verify("childSyncFlowChild");
  }

  @Test(expected = MessagingException.class)
  @Ignore("MULE-9731")
  public void childAsyncFlow() throws Exception {
    flowRunner("childAsyncFlow").withPayload(TEST_MESSAGE).asynchronously().run();
    verify("childAsyncFlowChild");
  }

  @Test
  @Ignore("MULE-9731")
  public void processorChain() throws Exception {
    assertVoidMuleEventResponse("processorChain");
  }

  @Test
  @Ignore("MULE-9731")
  public void filterAccepts() throws Exception {
    assertVoidMuleEventResponse("filterAccepts");
  }

  @Test
  @Ignore("MULE-9731")
  public void filterRejects() throws Exception {
    assertThat(flowRunner("filterRejects").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(), is(nullValue()));
  }

  @Test
  @Ignore("MULE-9731")
  public void filterAfterNonBlockingAccepts() throws Exception {
    assertVoidMuleEventResponse("filterAfterNonBlockingAccepts");
  }

  @Test
  @Ignore("MULE-9731")
  public void filterAfterNonBlockingRejects() throws Exception {
    assertVoidMuleEventResponse("filterAfterNonBlockingRejects");
  }

  @Test
  @Ignore("MULE-9731")
  public void filterBeforeNonBlockingAccepts() throws Exception {
    assertVoidMuleEventResponse("filterAfterNonBlockingAccepts");
  }

  @Test
  @Ignore("MULE-9731")
  public void filterBeforeNonBlockingRejects() throws Exception {
    assertThat(flowRunner("filterBeforeNonBlockingRejects").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(),
               is(nullValue()));
  }

  @Test
  @Ignore("MULE-9731")
  public void filterAfterEnricherBeforeNonBlocking() throws Exception {
    assertThat(flowRunner("filterAfterEnricherBeforeNonBlocking").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(),
               is(nullValue()));
  }

  @Test
  @Ignore("MULE-9731")
  public void securityFilter() throws Exception {
    assertVoidMuleEventResponse("security-filter");
  }

  @Test
  @Ignore("MULE-9731")
  public void transformer() throws Exception {
    assertVoidMuleEventResponse("transformer");
  }

  @Test
  @Ignore("MULE-9731")
  public void choice() throws Exception {
    assertVoidMuleEventResponse("choice");
  }

  @Test
  @Ignore("MULE-9731")
  public void enricher() throws Exception {
    assertVoidMuleEventResponse("enricher");
  }

  @Test
  @Ignore("MULE-9731")
  public void enricherIssue() throws Exception {
    assertVoidMuleEventResponse("enricherIssue");
  }

  @Test
  @Ignore("MULE-9731")
  public void enricherIssueNonBlocking() throws Exception {
    assertVoidMuleEventResponse("enricherIssueNonBlocking");
  }

  @Test
  @Ignore("MULE-9731")
  public void enricherFlowVar() throws Exception {
    assertVoidMuleEventResponse("enricherFlowVar");
  }

  @Test
  @Ignore("MULE-9731")
  public void async() throws Exception {
    assertVoidMuleEventResponse("async");
  }

  @Test
  @Ignore("MULE-9731")
  public void catchExceptionStrategy() throws Exception {
    assertVoidMuleEventResponse("catchExceptionStrategy");
    verify("catchExceptionStrategyChild");
  }

  private void assertVoidMuleEventResponse(String flowName) throws Exception {
    assertThat(flowRunner(flowName).withPayload(TEST_MESSAGE).asynchronously().run(), instanceOf(VoidMuleEvent.class));
  }

}

