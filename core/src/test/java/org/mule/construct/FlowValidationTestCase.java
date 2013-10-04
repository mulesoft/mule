/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.exception.RollbackMessagingExceptionStrategy;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

@RunWith(MockitoJUnitRunner.class)
public class FlowValidationTestCase extends AbstractMuleTestCase
{

    public static final String FLOW_NAME = "flowName";
    @Mock
    public MuleContext mockMuleContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    public InboundEndpoint inboundEndpoint;
    @Mock
    public RollbackMessagingExceptionStrategy rollbackMessagingExceptionStrategy;
    @Mock
    public AbstractRedeliveryPolicy mockRedeliveryPolicy;
    private Flow flow;

    @Before
    public void setUp()
    {
        this.flow = new Flow(FLOW_NAME, mockMuleContext);
    }

    @Test(expected = FlowConstructInvalidException.class)
    public void testProcessingStrategyCantBeAsyncWithRedelivery() throws Exception
    {
        configureFlowForRedelivery();
        flow.setProcessingStrategy(new AsynchronousProcessingStrategy());
        flow.validateConstruct();
    }

    @Test
    public void testChangeDefaultProcessingStrategyWithRedelivery() throws Exception
    {
        configureFlowForRedelivery();
        flow.validateConstruct();
        assertThat(flow.getProcessingStrategy(), instanceOf(SynchronousProcessingStrategy.class));
    }

    private void configureFlowForRedelivery()
    {
        when(inboundEndpoint.getTransactionConfig().isConfigured()).thenReturn(false);
        when(inboundEndpoint.getExchangePattern().hasResponse()).thenReturn(false);
        flow.setExceptionListener(rollbackMessagingExceptionStrategy);
        when(rollbackMessagingExceptionStrategy.hasMaxRedeliveryAttempts()).thenReturn(true);
        when(rollbackMessagingExceptionStrategy.acceptsAll()).thenReturn(true);
        when(inboundEndpoint.getRedeliveryPolicy()).thenReturn(mockRedeliveryPolicy);
        flow.setMessageSource(inboundEndpoint);
    }

}
