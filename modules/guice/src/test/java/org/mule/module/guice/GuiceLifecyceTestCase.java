/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.registry.AbstractLifecycleTracker;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.NullPayload;

import com.google.inject.AbstractModule;

import org.junit.Test;

public class GuiceLifecyceTestCase extends AbstractMuleContextTestCase
{
    @Override
    protected void doSetUp() throws Exception
    {
        GuiceConfigurationBuilder cb = new GuiceConfigurationBuilder(new GuiceLifecycleModule());
        cb.configure(muleContext);
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
        testComponentLifecycle("MuleSingletonService",
            "[setProperty, setMuleContext, setService, initialise, start, stop, dispose]");
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
        testComponentLifecycle("MulePrototypeService",
            "[setProperty, setMuleContext, setService, initialise, start, stop, dispose]");
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
        // Initialisation policy not enabled in iBeans
        // testComponentLifecycle("MulePooledSingletonService",
        // "[setProperty, setMuleContext, setService, initialise, initialise, initialise, start, start, start, stop, stop, stop, dispose, dispose, dispose]");
        testComponentLifecycle("MulePooledSingletonService",
            "[setProperty, setMuleContext, setService, initialise, start, stop, dispose]");
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
        MuleClient muleClient = muleContext.getClient();
        final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker)muleClient.send(
                "vm://" + serviceName + ".In", new DefaultMuleMessage(NullPayload.getInstance(), muleContext)).getPayload();

        assertNotNull(ltc);

        return ltc;
    }

    public class GuiceLifecycleModule extends AbstractModule
    {
        @Override
        protected void configure()
        {
            // no custom bindings
        }

        // @Provides @AnnotatedService
        // public PrototypeService createPrototypeService()
        // {
        // PrototypeService service = new PrototypeService();
        // service.setProperty("mps");
        // return service;
        // }
        //
        // @Provides @AnnotatedService @Singleton
        // public SingletonService createSingletonService()
        // {
        // SingletonService service = new SingletonService();
        // service.setProperty("mms");
        // return service;
        // }
        //
        // @Provides @AnnotatedService
        // public PooledService createPooledService()
        // {
        // PooledService service = new PooledService();
        // service.setProperty("mmps");
        // return service;
        // }
    }
}
