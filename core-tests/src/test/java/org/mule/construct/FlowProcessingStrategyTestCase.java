/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class FlowProcessingStrategyTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    protected MuleContext muleContext;

    @Mock
    private MuleConfiguration configuration;

    private AbstractPipeline flow;

    @Before
    public void before()
    {
        when(muleContext.getConfiguration()).thenReturn(configuration);
        createFlow();
    }

    @Test
    public void fixedProcessingStrategyIsHonoured() throws Exception
    {
        ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
        createFlow();
        flow.setProcessingStrategy(processingStrategy);

        assertThat(flow.getProcessingStrategy(), is(sameInstance(processingStrategy)));
    }

    @Test
    public void defaultProcessingStrategyInConfigIsHonoured() throws Exception
    {
        ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
        when(configuration.getDefaultProcessingStrategy()).thenReturn(processingStrategy);

        createFlow();
        assertThat(flow.getProcessingStrategy(), is(sameInstance(processingStrategy)));
    }

    @Test
    public void fixedProcessingStrategyTakesPrecedenceOverConfig() throws Exception
    {
        ProcessingStrategy configProcessingStrategy = mock(ProcessingStrategy.class);
        when(configuration.getDefaultProcessingStrategy()).thenReturn(configProcessingStrategy);

        ProcessingStrategy processingStrategy = mock(ProcessingStrategy.class);
        createFlow();
        flow.setProcessingStrategy(processingStrategy);

        assertThat(flow.getProcessingStrategy(), is(sameInstance(processingStrategy)));
    }

    @Test
    public void createDefaultProcessingStrategyIfNoneSpecified() throws Exception
    {
        flow.initialise();
        assertThat(flow.getProcessingStrategy(), is(instanceOf(DefaultFlowProcessingStrategy.class)));
    }

    private void createFlow()
    {
        flow = new Flow("test", muleContext);
    }
}
