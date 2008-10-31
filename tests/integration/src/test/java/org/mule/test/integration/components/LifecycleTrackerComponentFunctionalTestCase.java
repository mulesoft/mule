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
public class LifecycleTrackerComponentFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/components/component-lifecycle-config.xml";
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked
     * - Spring lifecycle methods invoked
     * - MuleContext is injected (Component implements MuleContextAware + Component is in registry)
     * - Service is injected (Component implements MuleContextAware + Component is in registry)
     * - Start + Stop lifecyle methods are invoked twice (See MULE-3888)
     * @throws Exception
     */
    public void testSpringBeanServiceLifecycle() throws Exception
    {
        testComponentLifecycle(
            "SpringBeanService",
            "[setProperty, setMuleContext, springInitialize, setService, initialise, start, start, stop, stop, dispose, springDestroy]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked
     * - Service is injected (Component implements ServiceAware)
     * @throws Exception
     */
    public void testSingletonServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MuleSingletonService",
            "[setProperty, setService, initialise, start, stop, dispose]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods invoked
     * - Service is injected (Component implements ServiceAware)
     * @throws Exception
     */
    public void testMulePrototypeServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MulePrototypeService",
            "[setProperty, setService, initialise, start, stop, dispose]");
    }

    /**
     * ASSERT:
     * - Mule lifecycle methods aren't invoked (They are invoked JIT with non-singleton components)
     * - Service is injected (Component implements ServiceAware)
     * @throws Exception
     */
    public void testMulePooledPrototypeServiceLifecycle() throws Exception
    {
        testComponentLifecycle("MulePooledPrototypeService", "[setProperty, setService, initialise, start, stop, dispose]");
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
        MuleClient muleClient = new MuleClient();
        final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker) muleClient.send(
            "vm://" + serviceName + ".In", null, null).getPayload();

        assertNotNull(ltc);

        return ltc;
    }
}
