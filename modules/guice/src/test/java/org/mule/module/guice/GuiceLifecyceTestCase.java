/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.guice;

import org.mule.module.client.MuleClient;
import org.mule.registry.AbstractLifecycleTracker;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.inject.AbstractModule;

import org.junit.Assert;
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

        Assert.assertEquals(serviceName, expectedLifeCycle, tracker.getTracker().toString());
    }

    private AbstractLifecycleTracker exerciseComponent(final String serviceName) throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);
        final AbstractLifecycleTracker ltc = (AbstractLifecycleTracker)muleClient.send(
            "vm://" + serviceName + ".In", null, null).getPayload();

        Assert.assertNotNull(ltc);

        return ltc;
    }

    public class GuiceLifecycleModule extends AbstractModule
    {
        @Override
        protected void configure()
        {

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
