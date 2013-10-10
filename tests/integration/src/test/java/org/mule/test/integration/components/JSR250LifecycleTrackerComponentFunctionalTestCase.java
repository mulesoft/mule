/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.components;

import org.mule.lifecycle.JSR250LifecycleTrackerComponent;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JSR250LifecycleTrackerComponentFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/components/jsr250-component-lifecycle-config-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/components/jsr250-component-lifecycle-config-flow.xml"}});
    }

    public JSR250LifecycleTrackerComponentFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
        if (variant.equals(ConfigVariant.FLOW))
        {
            testComponentLifecycle("MuleSingletonService",
                "[setProperty, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
        }
        else
        {
            testComponentLifecycle("MuleSingletonService",
                "[setProperty, setService, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
        }
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
        if (variant.equals(ConfigVariant.FLOW))
        {
            testComponentLifecycle("MulePrototypeService",
                "[setProperty, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
        }
        else
        {
            testComponentLifecycle("MulePrototypeService",
                "[setProperty, setService, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
        }
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
        if (variant.equals(ConfigVariant.FLOW))
        {
            testComponentLifecycle("MulePooledPrototypeService",
                "[setProperty, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
        }
        else
        {
            testComponentLifecycle("MulePooledPrototypeService",
                "[setProperty, setService, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
        }
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
        if (variant.equals(ConfigVariant.FLOW))
        {
            testComponentLifecycle(
                "MulePooledSingletonService",
                "[setProperty, setMuleContext, jsr250 initialise, jsr250 initialise, jsr250 initialise, start, start, start, stop, stop, stop, jsr250 dispose, jsr250 dispose, jsr250 dispose]");
        }
        else
        {
            testComponentLifecycle(
                "MulePooledSingletonService",
                "[setProperty, setService, setMuleContext, jsr250 initialise, jsr250 initialise, jsr250 initialise, start, start, start, stop, stop, stop, jsr250 dispose, jsr250 dispose, jsr250 dispose]");
        }
    }

    private void testComponentLifecycle(final String serviceName, final String expectedLifeCycle)
        throws Exception
    {

        final JSR250LifecycleTrackerComponent tracker = exerciseComponent(serviceName);

        muleContext.dispose();

        assertEquals(serviceName, expectedLifeCycle, tracker.getTracker().toString());
    }

    private JSR250LifecycleTrackerComponent exerciseComponent(final String serviceName) throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        final JSR250LifecycleTrackerComponent ltc = (JSR250LifecycleTrackerComponent) muleClient.send(
            "vm://" + serviceName + ".In", null, null).getPayload();

        assertNotNull(ltc);

        return ltc;
    }
}
