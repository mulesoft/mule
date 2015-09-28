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
import org.mule.api.MuleEvent;
import org.mule.tck.functional.FlowAssert;
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
    public void defaultFlow() throws Exception
    {
        testFlowNonBlocking("defaultFlow");
    }

    @Test
    public void nonBlockingFlow() throws Exception
    {
        testFlowNonBlocking("nonBlockingFlow");
    }

    @Test
    public void subFlow() throws Exception
    {
        testFlowNonBlocking("subFlow");
    }

    @Test
    public void childFlow() throws Exception
    {
        testFlowNonBlocking("childFlow");
    }

    @Test
    public void processorChain() throws Exception
    {
        testFlowNonBlocking("processorChain");
    }

    @Test
    public void filterAccepts() throws Exception
    {
        testFlowNonBlocking("filterAccepts");
    }

    @Test
    public void filterRejects() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterRejects");
        FlowAssert.verify("filterRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterAfterNonBlockingAccepts() throws Exception
    {
        testFlowNonBlocking("filterAfterNonBlockingAccepts");
    }

    @Test
    public void filterAfterNonBlockingRejects() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterNonBlockingRejects");
        FlowAssert.verify("filterAfterNonBlockingRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterBeforeNonBlockingAccepts() throws Exception
    {
        testFlowNonBlocking("filterAfterNonBlockingAccepts");
    }

    @Test
    public void filterBeforeNonBlockingRejects() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterNonBlockingRejects");
        FlowAssert.verify("filterAfterNonBlockingRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterAfterEnricherBeforeNonBlocking() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterEnricherBeforeNonBlocking");
        FlowAssert.verify("filterAfterEnricherBeforeNonBlocking");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void securityFilter() throws Exception
    {
        testFlowNonBlocking("security-filter");
    }

    @Test
    public void transformer() throws Exception
    {
        testFlowNonBlocking("transformer");
    }

    @Test
    public void choice() throws Exception
    {
        testFlowNonBlocking("choice");
    }

    @Test
    public void enricher() throws Exception
    {
        testFlowNonBlocking("enricher");
    }

    @Test
    public void response() throws Exception
    {
        testFlowNonBlocking("response");
    }

    @Test
    public void responseWithNullEvent() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("responseWithNullEvent");
        FlowAssert.verify("responseWithNullEvent");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void enricherIssue() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherIssue");
        FlowAssert.verify("enricherIssue");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void enricherIssueNonBlocking() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherIssueNonBlocking");
        FlowAssert.verify("enricherIssueNonBlocking");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void enricherFlowVar() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherFlowVar");
        FlowAssert.verify("enricherFlowVar");
        assertThat((String) result.getFlowVariable(FOO), is(equalTo(TEST_MESSAGE)));
    }
}

