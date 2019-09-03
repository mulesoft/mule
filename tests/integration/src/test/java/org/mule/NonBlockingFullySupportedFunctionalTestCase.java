/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.tck.functional.FlowAssert.verify;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class NonBlockingFullySupportedFunctionalTestCase extends FunctionalTestCase
{

    public static String FOO = "foo";

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-fully-supported-test-config.xml";
    }

    @Test
    public void flow() throws Exception
    {
        testFlowNonBlocking("flow", getMessageExchnagePattern());
    }

    @Test
    public void subFlow() throws Exception
    {
        testFlowNonBlocking("subFlow", getMessageExchnagePattern());
    }

    @Test
    public void childFlow() throws Exception
    {
        testFlowNonBlocking("childFlow", getMessageExchnagePattern());
        verify("childFlowChild");
    }

    @Test
    public void childDefaultFlow() throws Exception
    {
        testFlowNonBlocking("childDefaultFlow", getMessageExchnagePattern());
        verify("childDefaultFlowChild");
    }

    @Test
    public void childSyncFlow() throws Exception
    {
        testFlowNonBlocking("childSyncFlow", getMessageExchnagePattern());
        verify("childSyncFlowChild");
    }

    @Test(expected = MessagingException.class)
    public void childAsyncFlow() throws Exception
    {
        testFlowNonBlocking("childAsyncFlow", getMessageExchnagePattern());
        verify("childAsyncFlowChild");
    }

    @Test(expected = MessagingException.class)
    public void childQueuedAsyncFlow() throws Exception
    {
        testFlowNonBlocking("childQueuedAsyncFlow", getMessageExchnagePattern());
        verify("childQueuedAsyncFlowChild");
    }

    @Test
    public void processorChain() throws Exception
    {
        testFlowNonBlocking("processorChain", getMessageExchnagePattern());
    }

    @Test
    public void filterAccepts() throws Exception
    {
        testFlowNonBlocking("filterAccepts", getMessageExchnagePattern());
    }

    @Test
    public void filterRejects() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterRejects", getMessageExchnagePattern());
        verify("filterRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterAfterNonBlockingAccepts() throws Exception
    {
        testFlowNonBlocking("filterAfterNonBlockingAccepts", getMessageExchnagePattern());
    }

    @Test
    public void filterAfterNonBlockingRejects() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterNonBlockingRejects", getMessageExchnagePattern());
        verify("filterAfterNonBlockingRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterBeforeNonBlockingAccepts() throws Exception
    {
        testFlowNonBlocking("filterAfterNonBlockingAccepts", getMessageExchnagePattern());
    }

    @Test
    public void filterBeforeNonBlockingRejects() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterNonBlockingRejects", getMessageExchnagePattern());
        verify("filterAfterNonBlockingRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterAfterEnricherBeforeNonBlocking() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterEnricherBeforeNonBlocking", getMessageExchnagePattern());
        verify("filterAfterEnricherBeforeNonBlocking");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void securityFilter() throws Exception
    {
        testFlowNonBlocking("security-filter", getMessageExchnagePattern());
    }

    @Test
    public void transformer() throws Exception
    {
        testFlowNonBlocking("transformer", getMessageExchnagePattern());
    }

    @Test
    public void choice() throws Exception
    {
        testFlowNonBlocking("choice", getMessageExchnagePattern());
    }

    @Test
    public void enricher() throws Exception
    {
        testFlowNonBlocking("enricher", getMessageExchnagePattern());
    }

    @Test
    public void response() throws Exception
    {
        testFlowNonBlocking("response", getMessageExchnagePattern());
    }

    @Test
    public void responseWithNullEvent() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("responseWithNullEvent", getMessageExchnagePattern());
        verify("responseWithNullEvent");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void enricherIssue() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherIssue", getMessageExchnagePattern());
        verify("enricherIssue");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void enricherIssueNonBlocking() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherIssueNonBlocking", getMessageExchnagePattern());
        verify("enricherIssueNonBlocking");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void enricherFlowVar() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherFlowVar", getMessageExchnagePattern());
        verify("enricherFlowVar");
        assertThat((String) result.getFlowVariable(FOO), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void testTransportOutboundEndpoint() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("testOutboundEndpoint", getMessageExchnagePattern());
        verify("testOutboundEndpoint");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void testTransportOutboundEndpointError() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("testOutboundEndpointError", getMessageExchnagePattern());
        verify("testOutboundEndpointError");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void async() throws Exception
    {
        testFlowNonBlocking("async");
    }

    @Test
    public void catchExceptionStrategy() throws Exception
    {
        testFlowNonBlocking("catchExceptionStrategy", getMessageExchnagePattern());
        verify("catchExceptionStrategyChild");
    }

    @Test
    public void asyncThrowsErrorInsideErrorHandler() throws Exception
    {
        testFlowNonBlocking("asyncThrowsErrorInsideErrorHandler", getMessageExchnagePattern());
    }

    protected MessageExchangePattern getMessageExchnagePattern()
    {
        return MessageExchangePattern.REQUEST_RESPONSE;
    }

}

