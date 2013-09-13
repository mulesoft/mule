/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.schedule;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.tck.config.AbstractTestConfigurationFailure;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PollScheduleFrequencyTestCase extends AbstractTestConfigurationFailure
{

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"org/mule/test/integration/schedule/poll-scheduler-zero-frequency-config.xml", "zero frequency"}
        });
    }

    private static List<String> negativeFlowResponses = new ArrayList<String>();
    private static List<String> zeroFlowResponses = new ArrayList<String>();

    public PollScheduleFrequencyTestCase(String confResources, String expected)
    {
        super(confResources, expected);
    }

    @Rule
    public SystemProperty systemProperty = new SystemProperty("frequency.millis", "100");

    @Test
    public void testConfig()
    {
        try
        {
            muleContext.start();
            fail("Context was started properly but it was expected to fail");
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

    public static class ZeroComponent extends ComponentProcessor
    {

        public ZeroComponent()
        {
            super(zeroFlowResponses);
        }
    }
}