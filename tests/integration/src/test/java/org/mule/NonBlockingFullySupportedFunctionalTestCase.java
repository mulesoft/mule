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
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NonBlockingFullySupportedFunctionalTestCase extends FunctionalTestCase
{

    public static String FOO = "foo";
    private ProcessingStrategy processingStrategy;

    @Override
    protected String getConfigFile()
    {
        return "non-blocking-fully-supported-test-config.xml";
    }

    public NonBlockingFullySupportedFunctionalTestCase(ProcessingStrategy processingStrategy)
    {
        this.processingStrategy = processingStrategy;
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {new DefaultFlowProcessingStrategy()},
                {new NonBlockingProcessingStrategy()}}
        );
    }

    @Override
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        DefaultMuleConfiguration configuration = new DefaultMuleConfiguration();
        configuration.setDefaultProcessingStrategy(processingStrategy);
        contextBuilder.setMuleConfiguration(configuration);
    }

    @Test
    public void flow() throws Exception
    {
        testFlowNonBlocking("flow");
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
        verify("filterRejects");
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
        verify("filterAfterNonBlockingRejects");
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
        verify("filterAfterNonBlockingRejects");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void filterAfterEnricherBeforeNonBlocking() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("filterAfterEnricherBeforeNonBlocking");
        verify("filterAfterEnricherBeforeNonBlocking");
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
        verify("responseWithNullEvent");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void enricherIssue() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherIssue");
        verify("enricherIssue");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void enricherIssueNonBlocking() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherIssueNonBlocking");
        verify("enricherIssueNonBlocking");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void enricherFlowVar() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("enricherFlowVar");
        verify("enricherFlowVar");
        assertThat((String) result.getFlowVariable(FOO), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void testTransportOutboundEndpoint() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("testOutboundEndpoint");
        verify("testOutboundEndpoint");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

    @Test
    public void testTransportOutboundEndpointError() throws Exception
    {
        MuleEvent result = runFlowNonBlocking("testOutboundEndpointError");
        verify("testOutboundEndpointError");
        assertThat(result.getMessageAsString(), is(equalTo(TEST_MESSAGE)));
    }

}

