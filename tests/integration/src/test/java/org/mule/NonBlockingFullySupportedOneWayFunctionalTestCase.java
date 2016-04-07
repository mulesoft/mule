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
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class NonBlockingFullySupportedOneWayFunctionalTestCase extends FunctionalTestCase
{

    public static String FOO = "foo";

    @Override
    public int getTestTimeoutSecs()
    {
        return 600;
    }

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

    private void assertVoidMuleEventResponse(String flowName) throws Exception
    {
        assertThat(testFlow(flowName, ONE_WAY), instanceOf(VoidMuleEvent.class));
    }

}

