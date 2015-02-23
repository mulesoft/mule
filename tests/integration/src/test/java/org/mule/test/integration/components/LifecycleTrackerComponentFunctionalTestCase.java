/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.client.MuleClient;
import org.mule.lifecycle.AbstractLifecycleTracker;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author David Dossot (david@dossot.net) See
 *         http://mule.mulesoft.org/jira/browse/MULE-3846
 */
public class LifecycleTrackerComponentFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/components/component-lifecycle-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/components/component-lifecycle-config-flow.xml"}});
    }

    public LifecycleTrackerComponentFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * ASSERT: - Mule stop/start lifecycle methods invoked - Mule initialize/dipose
     * lifecycle methods NOT invoked - Spring lifecycle methods invoked - Service and
     * muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by
     * lifecycleAdaptor
     *
     * @throws Exception
     */
    @Test
    public void testSpringBeanServiceLifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, springInitialize, initialise, start, stop, dispose, springDestroy]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setMuleContext, springInitialize, initialise, setService, start, stop, dispose, springDestroy]";
        }

        testComponentLifecycle("SpringBeanService", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule stop/start lifecycle methods invoked - Mule initialize/dipose
     * lifecycle methods NOT invoked - Spring lifecycle methods NOT invoked - Service
     * and muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by
     * lifecycleAdaptor
     *
     * @throws Exception
     */
    @Test
    public void testSpringBeanService2Lifecycle() throws Exception
    {
        String expectedLifeCycle;

        if (variant.equals(ConfigVariant.FLOW))
        {
            expectedLifeCycle = "[setProperty, setMuleContext, initialise, start, stop, dispose]";
        }
        else
        {
            expectedLifeCycle = "[setProperty, setMuleContext, initialise, setService, start, stop, dispose]";
        }

        testComponentLifecycle("SpringBeanService2", expectedLifeCycle);
    }

    /**
     * ASSERT: - Mule lifecycle methods invoked - Service and muleContext injected
     * (Component implements ServiceAware/MuleContextAware)
     *
     * @throws Exception
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
     *
     * @throws Exception
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
     *
     * @throws Exception
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
     * new object in pool - Service and muleContext injected each time singleton is
     * used to create new object in pool (Component implements
     * ServiceAware/MuleContextAware)
     *
     * @throws Exception
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

        final AbstractLifecycleTracker tracker = exerciseComponent(serviceName);

        muleContext.dispose();

        assertEquals(serviceName, expectedLifeCycle, tracker.getTracker().toString());
    }

    private AbstractLifecycleTracker exerciseComponent(final String serviceName) throws Exception
    {
        MuleClient client = muleContext.getClient();
        final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker) client.send(
            "vm://" + serviceName + ".In", getTestMuleMessage(NullPayload.getInstance())).getPayload();

        assertNotNull(ltc);

        return ltc;
    }
}
