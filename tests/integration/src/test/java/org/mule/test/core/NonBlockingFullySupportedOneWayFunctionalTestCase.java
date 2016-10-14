/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class NonBlockingFullySupportedOneWayFunctionalTestCase extends AbstractIntegrationTestCase {

  public static String FOO = "foo";

  @Override
  protected String getConfigFile() {
    return "non-blocking-fully-supported-oneway-test-config.xml";
  }

  @Test
  public void flow() throws Exception {
    run("flow");
  }

  @Test
  public void subFlow() throws Exception {
    run("subFlow");
  }

  @Test
  public void childFlow() throws Exception {
    run("childFlow");
    verify("childFlowChild");
  }

  @Test
  public void childSyncFlow() throws Exception {
    run("childSyncFlow");
    verify("childSyncFlowChild");
  }

  public void childAsyncFlow() throws Exception {
    run("childAsyncFlow");
    verify("childAsyncFlowChild");
  }

  @Test
  public void processorChain() throws Exception {
    run("processorChain");
  }

  @Test
  public void filterAccepts() throws Exception {
    run("filterAccepts");
  }

  @Test
  public void filterRejects() throws Exception {
    assertThat(flowRunner("filterRejects").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(), is(nullValue()));
  }

  @Test
  public void filterAfterNonBlockingAccepts() throws Exception {
    run("filterAfterNonBlockingAccepts");
  }

  @Test
  public void filterAfterNonBlockingRejects() throws Exception {
    run("filterAfterNonBlockingRejects");
  }

  @Test
  public void filterBeforeNonBlockingAccepts() throws Exception {
    run("filterAfterNonBlockingAccepts");
  }

  @Test
  public void filterBeforeNonBlockingRejects() throws Exception {
    assertThat(flowRunner("filterBeforeNonBlockingRejects").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(),
               is(nullValue()));
  }

  @Test
  public void filterAfterEnricherBeforeNonBlocking() throws Exception {
    assertThat(flowRunner("filterAfterEnricherBeforeNonBlocking").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(),
               is(nullValue()));
  }

  @Test
  public void securityFilter() throws Exception {
    run("security-filter");
  }

  @Test
  public void transformer() throws Exception {
    run("transformer");
  }

  @Test
  public void choice() throws Exception {
    run("choice");
  }

  @Test
  public void enricher() throws Exception {
    run("enricher");
  }

  @Test
  public void enricherIssue() throws Exception {
    run("enricherIssue");
  }

  @Test
  public void enricherIssueNonBlocking() throws Exception {
    run("enricherIssueNonBlocking");
  }

  @Test
  public void enricherFlowVar() throws Exception {
    run("enricherFlowVar");
  }

  @Test
  public void async() throws Exception {
    run("async");
  }

  @Test
  @Ignore("MULE-10617")
  public void catchExceptionStrategy() throws Exception {
    run("catchExceptionStrategy");
    verify("catchExceptionStrategyChild");
  }

  private void run(String flowName) throws Exception {
    flowRunner(flowName).withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run();
  }

}

