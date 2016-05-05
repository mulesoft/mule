/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.functional.FlowAssert.verify;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MessagingException;

import org.junit.Test;

public class NonBlockingFullySupportedOneWayFunctionalTestCase extends FunctionalTestCase
{

    public static String FOO = "foo";

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-fully-supported-oneway-test-config.xml";
    }

    @Test
    public void flow() throws Exception
    {
        assertVoidMuleEventResponse("flow");
    }

    @Test
    public void subFlow() throws Exception
    {
        assertVoidMuleEventResponse("subFlow");
    }

    @Test
    public void childFlow() throws Exception
    {
        assertVoidMuleEventResponse("childFlow");
        verify("childFlowChild");
    }

    @Test
    public void childDefaultFlow() throws Exception
    {
        flowRunner("childDefaultFlow").withPayload(TEST_MESSAGE).asynchronously().run();
        verify("childDefaultFlowChild");
    }

    @Test
    public void childSyncFlow() throws Exception
    {
        flowRunner("childSyncFlow").withPayload(TEST_MESSAGE).asynchronously().run();
        verify("childSyncFlowChild");
    }

    @Test(expected = MessagingException.class)
    public void childAsyncFlow() throws Exception
    {
        flowRunner("childAsyncFlow").withPayload(TEST_MESSAGE).asynchronously().run();
        verify("childAsyncFlowChild");
    }

    @Test
    public void processorChain() throws Exception
    {
        assertVoidMuleEventResponse("processorChain");
    }

    @Test
    public void filterAccepts() throws Exception
    {
        assertVoidMuleEventResponse("filterAccepts");
    }

    @Test
    public void filterRejects() throws Exception
    {
        assertThat(flowRunner("filterRejects").withPayload(TEST_MESSAGE).asynchronously().nonBlocking().run(), is
                (nullValue()));
    }

    @Test
    public void filterAfterNonBlockingAccepts() throws Exception
    {
        assertVoidMuleEventResponse("filterAfterNonBlockingAccepts");
    }

    @Test
    public void filterAfterNonBlockingRejects() throws Exception
    {
        assertVoidMuleEventResponse("filterAfterNonBlockingRejects");
    }

    @Test
    public void filterBeforeNonBlockingAccepts() throws Exception
    {
        assertVoidMuleEventResponse("filterAfterNonBlockingAccepts");
    }

    @Test
    public void filterBeforeNonBlockingRejects() throws Exception
    {
        assertThat(flowRunner("filterBeforeNonBlockingRejects").withPayload(TEST_MESSAGE).asynchronously()
                           .nonBlocking().run(), is(nullValue()));
    }

    @Test
    public void filterAfterEnricherBeforeNonBlocking() throws Exception
    {
        assertThat(flowRunner("filterAfterEnricherBeforeNonBlocking").withPayload(TEST_MESSAGE).asynchronously()
                           .nonBlocking().run(), is(nullValue()));
    }

    @Test
    public void securityFilter() throws Exception
    {
        assertVoidMuleEventResponse("security-filter");
    }

    @Test
    public void transformer() throws Exception
    {
        assertVoidMuleEventResponse("transformer");
    }

    @Test
    public void choice() throws Exception
    {
        assertVoidMuleEventResponse("choice");
    }

    @Test
    public void enricher() throws Exception
    {
        assertVoidMuleEventResponse("enricher");
    }

    @Test
    public void enricherIssue() throws Exception
    {
        assertVoidMuleEventResponse("enricherIssue");
    }

    @Test
    public void enricherIssueNonBlocking() throws Exception
    {
        assertVoidMuleEventResponse("enricherIssueNonBlocking");
    }

    @Test
    public void enricherFlowVar() throws Exception
    {
        assertVoidMuleEventResponse("enricherFlowVar");
    }

    @Test
    public void async() throws Exception
    {
        assertVoidMuleEventResponse("async");
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        assertVoidMuleEventResponse("catchExceptionStrategy");
        verify("catchExceptionStrategyChild");
    }

    private void assertVoidMuleEventResponse(String flowName) throws Exception
    {
        assertThat(flowRunner(flowName).withPayload(TEST_MESSAGE).asynchronously().run(), instanceOf(VoidMuleEvent.class));
    }

}

