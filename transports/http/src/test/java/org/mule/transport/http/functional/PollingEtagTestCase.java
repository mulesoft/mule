/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class PollingEtagTestCase extends AbstractServiceAndFlowTestCase
{
    private static final int WAIT_TIME = 2500;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Test
    @Ignore("MULE-6926: flaky test")
    public void testPollingReceiversRestart() throws Exception
    {
        Object ftc = getComponent("Test");
        assertTrue("FunctionalTestComponent expected", ftc instanceof FunctionalTestComponent);

        AtomicInteger pollCounter = new AtomicInteger(0);
        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

        // should be enough to poll for multiple messages
        Thread.sleep(WAIT_TIME);

        assertEquals(1, pollCounter.get());
    }

    public PollingEtagTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "polling-etag-test-service.xml"},
            {ConfigVariant.FLOW, "polling-etag-test-flow.xml"}
        });
    }      
    
}

