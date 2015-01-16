/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertTrue;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;

public class PollingReceiversRestartTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public PollingReceiversRestartTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "polling-receivers-restart-test-flow.xml";
    }

    @Test
    public void testPollingReceiversRestart() throws Exception
    {
        muleContext.start();

        Object ftc = getComponent("Test");
        assertTrue("FunctionalTestComponent expected", ftc instanceof FunctionalTestComponent);

        final AtomicInteger pollCounter = new AtomicInteger(0);
        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

        Probe probeForPollEvents = new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return pollCounter.get() > 0;
            }

            @Override
            public String describeFailure()
            {
                return "No polls performed";
            }
        };

        new PollingProber(10000, 100).check(probeForPollEvents);

        // stop and restart
        muleContext.stop();

        pollCounter.set(0);
        muleContext.start();

        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));
        new PollingProber(10000, 100).check(probeForPollEvents);

        muleContext.dispose();
    }
}
