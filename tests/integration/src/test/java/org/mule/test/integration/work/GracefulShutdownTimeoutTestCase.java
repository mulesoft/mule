/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.work;

import org.mule.api.MuleEventContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.service.Service;
import org.mule.construct.Flow;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

public class GracefulShutdownTimeoutTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/work/graceful-shutdown-timeout-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/work/graceful-shutdown-timeout-flow.xml"}});
    }

    public GracefulShutdownTimeoutTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
    @Test
    public void testGracefulShutdownTimeout() throws Exception
    {
        final Latch latch = new Latch();

        FlowConstruct service = muleContext.getRegistry().lookupFlowConstruct("TestService");
        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(service);
        testComponent.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                Thread.sleep(5500);
                latch.countDown();

            }
        });

        if (variant.equals(ConfigVariant.FLOW))
        {
            ((Flow) service).process(getTestEvent("test"));
            Thread.sleep(200);
            ((Flow) service).dispose();
            assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        }
        else
        {
            ((Service) service).dispatchEvent(getTestEvent("test"));
            Thread.sleep(200);
            ((Service) service).dispose();
            assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        }
    }
}
