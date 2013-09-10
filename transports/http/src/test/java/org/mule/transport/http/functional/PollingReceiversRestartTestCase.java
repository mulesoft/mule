/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;

public class PollingReceiversRestartTestCase extends AbstractServiceAndFlowTestCase
{

    private static final int WAIT_TIME = 3000;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public PollingReceiversRestartTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setStartContext(false);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "polling-receivers-restart-test-service.xml"},
            {ConfigVariant.FLOW, "polling-receivers-restart-test-flow.xml"}
        });
    }

    @Test
    public void testPollingReceiversRestart() throws Exception
    {
        muleContext.start();

        Object ftc = getComponent("Test");
        assertTrue("FunctionalTestComponent expected", ftc instanceof FunctionalTestComponent);

        AtomicInteger pollCounter = new AtomicInteger(0);
        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

        // should be enough to poll for 2 messages
        Thread.sleep(WAIT_TIME);

        // stop
        muleContext.stop();
        assertTrue("No polls performed", pollCounter.get() > 0);

        // and restart
        muleContext.start();

        pollCounter.set(0);
        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

        Thread.sleep(WAIT_TIME);
        muleContext.dispose();
        assertTrue("No polls performed", pollCounter.get() > 0);
    }

}
