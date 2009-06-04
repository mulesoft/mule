/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.work;

import org.mule.api.MuleEventContext;
import org.mule.api.service.Service;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class GracefulShutdownTimeoutTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/work/graceful-shutdown-timeout.xml";
    }

    @Override
    protected boolean isGracefulShutdown()
    {
        return true;
    }

    /**
     * Dispatch an event to a service component that takes longer than default
     * graceful shutdown time to complete and customize the graceful shutdown timeout
     * in configuration so that component execution is not interrupted. This tests
     * services but the same applies to the graceful shutdown of
     * receivers/dispatchers etc.
     * 
     * @throws Exception
     */
    public void testGracefulShutdownTimeout() throws Exception
    {
        final Latch latch = new Latch();
        org.mule.api.service.Service service = (Service) muleContext.getRegistry().lookupService(
            "TestService");
        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(service);
        testComponent.setEventCallback(new EventCallback()
        {

            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                Thread.sleep(5500);
                latch.countDown();

            }
        });
        service.dispatchEvent(getTestInboundEvent("test"));
        Thread.sleep(200);
        service.dispose();
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
    }

}
