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

import org.mule.MuleServer;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.model.UMOModel;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class PollingReceiversRestartTestCase extends AbstractMuleTestCase
{
    private static final int WAIT_TIME = 2500;

    public void testPollingReceiversRestart() throws Exception
    {
        // we are going to stop and start Mule in this thread, make it
        // a daemon so the test can exit properly
        MuleServer mule = new MuleServer("polling-receivers-restart-test.xml");
        mule.start(true, false);

        // well, no way to register notification on the MuleServer, only
        // possible for MuleManager, so just sleep
        Thread.sleep(2000);

        UMOModel model = (UMOModel) managementContext.getRegistry().getModels().get("main");
        FunctionalTestComponent ftc = (FunctionalTestComponent) model.getComponent("Test").getInstance();
        assertNotNull("Functional Test Component not found in the model.", ftc);

        AtomicInteger pollCounter = new AtomicInteger(0);
        ftc.setEventCallback(new CounterCallback(pollCounter));

       managementContext.start();
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
        //manager.dispose();
        assertTrue("No polls performed", pollCounter.get() > 0);
    }


}

