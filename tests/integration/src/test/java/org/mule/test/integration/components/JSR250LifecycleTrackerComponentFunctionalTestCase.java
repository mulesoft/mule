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
import org.mule.lifecycle.JSR250LifecycleTrackerComponent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class JSR250LifecycleTrackerComponentFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/components/jsr250-component-lifecycle-config-flow.xml";
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
        testComponentLifecycle("MuleSingletonService", "[setProperty, setFlowConstruct, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
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
        testComponentLifecycle("MulePrototypeService", "[setProperty, setFlowConstruct, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
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
        testComponentLifecycle("MulePooledPrototypeService", "[setProperty, setFlowConstruct, setMuleContext, jsr250 initialise, start, stop, jsr250 dispose]");
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
        testComponentLifecycle("MulePooledSingletonService",
                               "[setProperty, setFlowConstruct, setMuleContext, jsr250 initialise, jsr250 initialise, jsr250 initialise, start, start, start, stop, stop, stop, jsr250 dispose, jsr250 dispose, jsr250 dispose]");
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
        MuleClient client = muleContext.getClient();
        final JSR250LifecycleTrackerComponent ltc = (JSR250LifecycleTrackerComponent) client.send(
                "vm://" + serviceName + ".In", null, null).getPayload();

        assertNotNull(ltc);
        return ltc;
    }
}
