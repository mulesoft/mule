/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.http.functional;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class PollingReceiversRestartTestCase extends FunctionalTestCase
{
    private static final int WAIT_TIME = 2500;

    public PollingReceiversRestartTestCase()
    {
        setStartContext(false);
    }

    protected String getConfigResources()
    {
        return "polling-receivers-restart-test.xml";
    }

    public void testPollingReceiversRestart() throws Exception
    {

        managementContext.start();

        FunctionalTestComponent ftc = lookupTestComponent("main","Test");

        AtomicInteger pollCounter = new AtomicInteger(0);
        ftc.setEventCallback(new CounterCallback(pollCounter));

        // should be enough to poll for 2 messages
        Thread.sleep(WAIT_TIME);

        // stop
        managementContext.stop();
        assertTrue("No polls performed", pollCounter.get() > 0);

        // and restart
        managementContext.start();

        pollCounter.set(0);
        ftc.setEventCallback(new CounterCallback(pollCounter));

        Thread.sleep(WAIT_TIME);
        managementContext.dispose();
        assertTrue("No polls performed", pollCounter.get() > 0);
    }


}

