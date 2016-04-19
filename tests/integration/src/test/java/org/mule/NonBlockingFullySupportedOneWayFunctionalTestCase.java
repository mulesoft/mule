/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.MessageExchangePattern.ONE_WAY;
import static org.mule.tck.functional.FlowAssert.verify;
import org.mule.api.MessagingException;
import org.mule.tck.junit4.FunctionalTestCase;

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
        testFlow("childDefaultFlow", ONE_WAY);
        verify("childDefaultFlowChild");
    }

    @Test
    public void childSyncFlow() throws Exception
    {
        testFlow("childSyncFlow", ONE_WAY);
        verify("childSyncFlowChild");
    }

    @Test(expected = MessagingException.class)
    public void childAsyncFlow() throws Exception
    {
        testFlow("childAsyncFlow", ONE_WAY);
        verify("childAsyncFlowChild");
    }

    @Test(expected = MessagingException.class)
    public void childQueuedAsyncFlow() throws Exception
    {
        assertThat(testFlow("childQueuedAsyncFlow", ONE_WAY), instanceOf(DefaultMuleEvent.class));
        verify("childQueuedAsyncFlowChild");
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
        assertThat(testFlow("filterRejects", ONE_WAY), is(nullValue()));
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
        assertThat(testFlow("filterBeforeNonBlockingRejects", ONE_WAY), is(nullValue()));
    }

    @Test
    public void filterAfterEnricherBeforeNonBlocking() throws Exception
    {
        assertThat(testFlow("filterAfterEnricherBeforeNonBlocking", ONE_WAY), is(nullValue()));
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
    public void testTransportOutboundEndpoint() throws Exception
    {
        assertVoidMuleEventResponse("testOutboundEndpoint");
    }

    @Test
    public void testTransportOutboundEndpointError() throws Exception
    {
        assertVoidMuleEventResponse("testOutboundEndpointError");
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
        assertThat(testFlow(flowName, ONE_WAY), instanceOf(VoidMuleEvent.class));
    }

}

