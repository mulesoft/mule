/*
 * $Id\$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.schedule;


import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.tck.config.AbstractTestConfigurationFailure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PollScheduleFrequencyTestCase extends AbstractTestConfigurationFailure
{

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"org/mule/test/integration/schedule/poll-scheduler-zero-frequency-config.xml", "zero frequency"},
                {"org/mule/test/integration/schedule/poll-scheduler-negative-frequency-config.xml", "negative frequency"}
        });
    }

    private static List<String> negative = new ArrayList<String>();
    private static List<String> zero = new ArrayList<String>();

    public PollScheduleFrequencyTestCase(String confResources, String expected)
    {
        super(confResources, expected);
    }

    @Override
    public void doSetUp()
    {
        System.setProperty("frequency.millis", "100");
    }

    @Test
    public void testConfig()
    {
        try
        {
            startMuleContext();
        }
        catch (MuleException mExp)
        {
            Throwable e = mExp;
            while (e.getCause() != null)
            {
                e = e.getCause();
            }
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    public static class NegativeComponent
    {

        public boolean process(String s)
        {
            synchronized (negative)
            {

                if (negative.size() < 10)
                {
                    negative.add(s);
                    return true;
                }
            }
            return false;
        }
    }

    public static class ZeroComponent
    {

        public boolean process(String s)
        {
            synchronized (zero)
            {

                if (zero.size() < 10)
                {
                    zero.add(s);
                    return true;
                }
            }
            return false;
        }
    }
}