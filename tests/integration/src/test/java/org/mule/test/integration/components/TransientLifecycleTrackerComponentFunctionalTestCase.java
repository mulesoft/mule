/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.components;

import org.mule.lifecycle.AbstractLifecycleTracker;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author David Dossot (david@dossot.net) See
 *         http://mule.mulesoft.org/jira/browse/MULE-3846
 */
public class TransientLifecycleTrackerComponentFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/components/component-lifecycle-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/components/component-lifecycle-config-flow.xml"}});
    }

    public TransientLifecycleTrackerComponentFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * ASSERT: - Mule stop/start lifecycle methods invoked - Mule initialize/dipose
     * lifecycle methods NOT invoked - Spring lifecycle methods invoked - Service and
     * muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by
     * lifecycleAdaptor
     */
    @Test
    public void testSpringBeanServiceLifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, springInitialize, start, stop, springDestroy]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setMuleContext, springInitialize, setService, start, stop, springDestroy]";
        }

        testComponentLifecycle("SpringBeanService", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule stop/start lifecycle methods invoked - Mule initialize/dipose
     * lifecycle methods NOT invoked - Spring lifecycle methods NOT invoked - Service
     * and muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by
     * lifecycleAdaptor
     */
    @Test
    public void testSpringBeanService2Lifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, start, stop]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setMuleContext, setService, start, stop]";
        }

        testComponentLifecycle("SpringBeanService2", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected
     * (Component implements ServiceAware/MuleContextAware)
     */
    @Test
    public void testSingletonServiceLifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, initialise, start, stop, dispose]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setService, setMuleContext, initialise, start, stop, dispose]";
        }

        testComponentLifecycle("MuleSingletonService", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected
     * (Component implements ServiceAware/MuleContextAware)
     */
    @Test
    public void testMulePrototypeServiceLifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, initialise, start, stop, dispose]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setService, setMuleContext, initialise, start, stop, dispose]";
        }

        testComponentLifecycle("MulePrototypeService", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected
     * (Component implements ServiceAware/MuleContextAware)
     */
    @Test
    public void testMulePooledPrototypeServiceLifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, initialise, start, stop, dispose]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setService, setMuleContext, initialise, start, stop, dispose]";
        }

        testComponentLifecycle("MulePooledPrototypeService", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule lifecycle methods invoked each time singleton is used to create
     * new object in pool NOTE: injecting service/muleContext only once since this is
     * a pooled singleton
     */
    @Test
    public void testMulePooledSingletonServiceLifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, initialise, initialise, initialise, start, start, start, stop, stop, stop, dispose, dispose, dispose]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setService, setMuleContext, initialise, initialise, initialise, start, start, start, stop, stop, stop, dispose, dispose, dispose]";
        }

        testComponentLifecycle("MulePooledSingletonService", expectedLifeCycle);
    }

    private void testComponentLifecycle(final String serviceName, final String expectedLifeCycle)
        throws Exception
    {
        AbstractLifecycleTracker tracker = exerciseComponent(serviceName);

        muleContext.dispose();

        assertEquals(serviceName, expectedLifeCycle, tracker.getTracker().toString());
    }

    private AbstractLifecycleTracker exerciseComponent(final String serviceName) throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker) muleClient.send(
            "vm://" + serviceName + ".In", null, null).getPayload();

        assertNotNull(ltc);

        return ltc;
    }
}
