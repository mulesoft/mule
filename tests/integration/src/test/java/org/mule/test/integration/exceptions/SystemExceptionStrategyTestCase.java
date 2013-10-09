/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.lifecycle.LifecycleException;
import org.mule.tck.testmodels.mule.TestConnector;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SystemExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/system-exception-strategy.xml";
    }

    @Test
    public void testConnectorStartup() throws Exception
    {
        try
        {
            TestConnector c = (TestConnector) muleContext.getRegistry().lookupConnector("testConnector");
            c.setInitialStateStopped(false);
            c.start();
            fail("Connector should have thrown an exception");
        }
        catch (LifecycleException e)
        {
            // expected
        }
        latch.await(1000, TimeUnit.MILLISECONDS); 
        assertEquals(0, serviceExceptionCounter.get());
        assertEquals(1, systemExceptionCounter.get());
    }

    @Test
    public void testConnectorPolling() throws Exception
    {
        muleContext.getRegistry().lookupService("Polling").start();
        Thread.sleep(3000);
        latch.await(1000, TimeUnit.MILLISECONDS); 
        assertEquals(0, serviceExceptionCounter.get());
        assertEquals(1, systemExceptionCounter.get());
    }
}


