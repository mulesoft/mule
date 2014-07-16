/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MuleEventCopyPerformanceTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    @Mock
    private Flow flow;

    private String payload;
    private DefaultMuleEvent event;

    @Override
    public int getTestTimeoutSecs()
    {
        return 120;
    }

    @Before
    public void before() throws IOException
    {
        payload = IOUtils.getResourceAsString("test-data.json", getClass());
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copy() throws Exception
    {
        event = createMuleEvent();
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) event.newThreadCopy();
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith10FlowVarsAnd10Properties() throws Exception
    {
        event = createMuleEventWithFlowVarsAndProperties(10);
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) event.newThreadCopy();
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith50FlowVarsAnd50Properties() throws Exception
    {
        event = createMuleEventWithFlowVarsAndProperties(50);
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) event.newThreadCopy();
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith10FlowVarsAnd10PopertiesWrite1OfEach() throws Exception
    {
        event = createMuleEventWithFlowVarsAndProperties(10);
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) event.newThreadCopy();
            event.setFlowVariable("newKey", "val");
            event.getMessage().setOutboundProperty("newKey", "val");
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith10FlowVarsAnd10PopertiesWrite5OfEach() throws Exception
    {
        DefaultMuleEvent original = createMuleEventWithFlowVarsAndProperties(10);
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) original.newThreadCopy();
            for (int j = 1; j <= 5; j++)
            {
                event.setFlowVariable("newKey" + j, "val");
                event.getMessage().setOutboundProperty("newKey" + j, "val");
            }
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith50FlowVarsAnd50PropertiesWrite1OfEach() throws Exception
    {
        DefaultMuleEvent original = createMuleEventWithFlowVarsAndProperties(50);
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) original.newThreadCopy();
            event.setFlowVariable("newKey", "val");
            event.getMessage().setOutboundProperty("newKey", "val");
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith100FlowVarsAndPropertiesWrite25OfEach() throws Exception
    {
        DefaultMuleEvent original = createMuleEventWithFlowVarsAndProperties(50);
        for (int i = 0; i < 1000; i++)
        {
            event = (DefaultMuleEvent) original.newThreadCopy();
            for (int j = 1; j <= 25; j++)
            {
                event.setFlowVariable("newKey" + j, "val");
                event.getMessage().setOutboundProperty("newKey" + j, "val");
            }
        }
    }

    protected DefaultMuleEvent createMuleEvent() throws Exception
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(payload, muleContext),
            MessageExchangePattern.ONE_WAY, flow);
    }

    protected DefaultMuleEvent createMuleEventWithFlowVarsAndProperties(int numProperties) throws Exception
    {
        DefaultMuleEvent event = createMuleEvent();
        for (int i = 1; i <= numProperties; i++)
        {
            event.setFlowVariable("InBoUnDpRoPeRtYkEy" + i, "val");
            event.getMessage().setOutboundProperty("InBoUnDpRoPeRtYkEy" + i, "val");
        }
        return event;
    }

}
