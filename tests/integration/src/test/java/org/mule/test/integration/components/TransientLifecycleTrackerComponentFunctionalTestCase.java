/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.components;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

/**
 * @author David Dossot (david@dossot.net)
 * See http://mule.mulesource.org/jira/browse/MULE-3846
 */
public class TransientLifecycleTrackerComponentFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/components/component-lifecycle-config.xml";
    }

    /**
     * ASSERT:
     * - Mule stop/start lifecycle methods invoked
     * - Mule initialize/dipose lifecycle methods NOT invoked
     * - Spring lifecycle methods invoked
     * - Service and muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by lifecycleAdaptor
     */
    public void testSpringBeanServiceLifecycle() throws Exception
    {
        testComponentLifecycle("SpringBeanService",
            "[setProperty, setMuleContext, springInitialize, setService, start, stop, springDestroy]");
    }

    /**
     * ASSERT:
     * - Mule stop/start lifecycle methods invoked
     * - Mule initialize/dipose lifecycle methods NOT invoked
     * - Spring lifecycle methods NOT invoked
     * - Service and muleContext injected (Component implements ServiceAware/MuleContextAware)
     * NOTE: muleContext is injected twice, once by registry and once by lifecycleAdaptor
     */
    public void testSpringBeanService2Lifecycle() throws Exception
    {
        testComponentLifecycle("SpringBeanService2",
            "[setProperty, setMuleContext, setService, start, stop]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked
     * - Service and muleContext injected (Component implements ServiceAware/MuleContextAware)
     */
    public void testSingletonServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MuleSingletonService",
            "[setProperty, setService, setMuleContext, initialise, start, stop, dispose]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked
     * - Service and muleContext injected (Component implements ServiceAware/MuleContextAware)
     */
    public void testMulePrototypeServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MulePrototypeService",
            "[setProperty, setService, setMuleContext, initialise, start, stop, dispose]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked
     * - Service and muleContext injected (Component implements ServiceAware/MuleContextAware)
     */
    public void testMulePooledPrototypeServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MulePooledPrototypeService", 
            "[setProperty, setService, setMuleContext, initialise, start, stop, dispose]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked each time singleton is used to create new object in pool
     * NOTE: injecting service/muleContext only once since this is a pooled singleton
     */
    public void testMulePooledSingletonServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MulePooledSingletonService", 
            "[setProperty, setService, setMuleContext, initialise, initialise, initialise, start, start, start, stop, stop, stop, dispose, dispose, dispose]");
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
        MuleClient muleClient = new MuleClient();
        final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker) muleClient.send(
            "vm://" + serviceName + ".In", null, null).getPayload();

        assertNotNull(ltc);

        return ltc;
    }
}