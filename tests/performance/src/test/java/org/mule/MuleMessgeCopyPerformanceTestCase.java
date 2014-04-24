/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
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
public class MuleMessgeCopyPerformanceTestCase extends AbstractMuleTestCase
{
    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    @Mock
    private MuleContext muleContext;

    private String payload;
    private MuleMessage message;

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
    public void copy()
    {
        MuleMessage original = createMuleMessage();
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith20Poperties()
    {
        MuleMessage original = createMuleMessageWithProperties(10);
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith100Poperties()
    {
        MuleMessage original = createMuleMessageWithProperties(50);
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith20PopertiesWrite1Outbound()
    {
        MuleMessage original = createMuleMessageWithProperties(10);
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
            message.setProperty("newKey", "val", PropertyScope.OUTBOUND);
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith20PopertiesWrite10Outbound()
    {
        MuleMessage original = createMuleMessageWithProperties(10);
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
            for (int j = 1; j <= 10; j++)
            {
                message.setProperty("newKey" + i, "val", PropertyScope.INBOUND);
            }
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith100PopertiesWrite1Outbound()
    {
        MuleMessage original = createMuleMessageWithProperties(50);
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
            message.setProperty("newKey", "val", PropertyScope.OUTBOUND);
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void copyWith100PopertiesWrite50Outbound()
    {
        MuleMessage original = createMuleMessageWithProperties(50);
        for (int i = 0; i < 1000; i++)
        {
            message = new DefaultMuleMessage(original);
            for (int j = 1; j <= 50; j++)
            {
                message.setProperty("newKey" + i, "val", PropertyScope.INBOUND);
            }
        }
    }

    protected MuleMessage createMuleMessage()
    {
        return new DefaultMuleMessage(payload, muleContext);
    }

    protected MuleMessage createMuleMessageWithProperties(int numProperties)
    {
        MuleMessage message = createMuleMessage();
        for (int i = 1; i <= numProperties; i++)
        {
            message.setProperty("InBoUnDpRoPeRtYkEy" + i, "val", PropertyScope.INBOUND);
        }
        for (int i = 1; i <= numProperties; i++)
        {
            message.setProperty("OuTBoUnDpRoPeRtYkEy" + i, "val", PropertyScope.OUTBOUND);
        }
        return message;
    }

}
