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
import org.mule.impl.model.MuleProxy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestMuleProxy;
import org.mule.tck.testmodels.mule.TestSedaComponent;
import org.mule.umo.UMOComponent;
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
        Thread.sleep(WAIT_TIME);

        UMOModel model = (UMOModel) managementContext.getRegistry().getModels().get("main");

        UMOComponent c = model.getComponent("Test");
        assertTrue("Component should be a TestSedaComponent", c instanceof TestSedaComponent);
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        Object ftc = ((TestMuleProxy) proxy).getComponent();
        assertNotNull("Functional Test Component not found in the model.", ftc);
        assertTrue("Service should be a FunctionalTestComponent", ftc instanceof FunctionalTestComponent);

        AtomicInteger pollCounter = new AtomicInteger(0);
        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

        managementContext.start();
        // should be enough to poll for 2 messages
        Thread.sleep(WAIT_TIME);

        // stop
        managementContext.stop();
        assertTrue("No polls performed", pollCounter.get() > 0);

        // and restart
        managementContext.start();

        pollCounter.set(0);
        ((FunctionalTestComponent) ftc).setEventCallback(new CounterCallback(pollCounter));

        Thread.sleep(WAIT_TIME);
        managementContext.dispose();
        assertTrue("No polls performed", pollCounter.get() > 0);
    }


}

