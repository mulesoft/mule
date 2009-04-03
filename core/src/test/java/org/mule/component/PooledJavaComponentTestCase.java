/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.api.component.LifecycleAdapter;
import org.mule.config.PoolingProfile;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.services.UniqueComponent;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.util.ExceptionUtils;

public class PooledJavaComponentTestCase extends AbstractComponentTestCase
{

    public static final byte MAX_ACTIVE = 3;
    public static final long MAX_WAIT = 1500;

    protected PoolingProfile getDefaultPoolingProfile()
    {
        PoolingProfile pp = new PoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        pp.setMaxActive(MAX_ACTIVE);
        pp.setMaxWait(MAX_WAIT);
        pp.setInitialisationPolicy(PoolingProfile.INITIALISE_NONE);
        return pp;
    }

    // @Override
    protected PrototypeObjectFactory getObjectFactory()
    {
        return new PrototypeObjectFactory(Orange.class);
    }

    // @Override
    public void testComponentCreation() throws Exception
    {
        PrototypeObjectFactory objectFactory = getObjectFactory();
        objectFactory.setObjectClass(Orange.class);
        objectFactory.initialise();

        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);

        PooledJavaComponent component = new PooledJavaComponent(objectFactory, pp);
        assertNotNull(component.getObjectFactory());
        assertEquals(objectFactory, component.getObjectFactory());
        assertEquals(Orange.class, component.getObjectFactory().getObjectClass());
        assertEquals(Orange.class, component.getObjectType());
        assertNotNull(component.getPoolingProfile());
        assertEquals(pp, component.getPoolingProfile());
        assertEquals(PoolingProfile.WHEN_EXHAUSTED_FAIL, component.getPoolingProfile().getExhaustedAction());
    }

    public void testPoolCreation() throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), getDefaultPoolingProfile());
        assertNull(component.lifecycleAdapterPool);
        component.setService(getTestService());
        component.initialise();
        assertNull(component.lifecycleAdapterPool);
        component.start();
        assertNotNull(component.lifecycleAdapterPool);
        component.stop();
        assertNull(component.lifecycleAdapterPool);
    }

    // @Override
    public void test() throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), getDefaultPoolingProfile());
        component.setService(getTestService());
        component.initialise();
        component.start();
        assertNotSame(component.borrowComponentLifecycleAdaptor(), component.borrowComponentLifecycleAdaptor());
        component.stop();
        component.start();
        assertNotSame(((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get(),
            ((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get());
    }

    public void testCreatePool() throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), getDefaultPoolingProfile());
        component.setService(getTestService());
        component.initialise();
        component.start();

        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        LifecycleAdapter borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        assertEquals(1, component.lifecycleAdapterPool.getNumActive());
        component.returnComponentLifecycleAdaptor(borrowed);
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        assertEquals(1, component.lifecycleAdapterPool.getNumActive());
        Object borrowed2 = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed2);
        assertEquals(2, component.lifecycleAdapterPool.getNumActive());
    }

    public void testFailOnExhaust() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), pp);
        component.setService(getTestService());
        component.initialise();
        component.start();

        Object borrowed = null;

        for (int i = 0; i < MAX_ACTIVE; i++)
        {
            borrowed = component.borrowComponentLifecycleAdaptor();
            assertNotNull(borrowed);
            assertEquals(component.lifecycleAdapterPool.getNumActive(), i + 1);
        }

        try
        {
            borrowed = component.borrowComponentLifecycleAdaptor();
            fail("Should throw an Exception");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testBlockExpiryOnExhaust() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), pp);
        component.setService(getTestService());
        component.initialise();
        component.start();

        Object borrowed = null;

        assertEquals(0, component.lifecycleAdapterPool.getNumActive());
        borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        assertEquals(3, component.lifecycleAdapterPool.getNumActive());

        // TODO
        // long starttime = System.currentTimeMillis();
        try
        {
            borrowed = component.borrowComponentLifecycleAdaptor();
            fail("Should throw an Exception");
        }
        catch (Exception e)
        {
            // TODO
            // long totalTime = System.currentTimeMillis() - starttime;
            // Need to allow for alittle variance in system time
            // This is unreliable
            // assertTrue(totalTime < (DEFAULT_WAIT + 300) && totalTime >
            // (DEFAULT_WAIT - 300));
        }
    }

    public void testBlockOnExhaust() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), pp);
        component.setService(getTestService());
        component.initialise();
        component.start();

        Object borrowed = null;

        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        borrowed = component.borrowComponentLifecycleAdaptor();
        borrowed = component.borrowComponentLifecycleAdaptor();
        assertEquals(2, component.lifecycleAdapterPool.getNumActive());

        // TODO
        // long starttime = System.currentTimeMillis();
        long borrowerWait = 500;
        Borrower borrower = new Borrower(component, borrowerWait);
        borrower.start();
        // Make sure the borrower borrows first
        try
        {
            Thread.sleep(200);
        }
        catch (InterruptedException e)
        {
            // ignore
        }

        borrowed = component.borrowComponentLifecycleAdaptor();
        // TODO
        // long totalTime = System.currentTimeMillis() - starttime;
        // Need to allow for alittle variance in system time
        // This is unreliable
        // assertTrue(totalTime < (borrowerWait + 300) && totalTime >
        // (borrowerWait -300));

        assertNotNull(borrowed);
    }

    public void testGrowOnExhaust() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_GROW);
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), pp);
        component.setService(getTestService());
        component.initialise();
        component.start();

        Object borrowed = component.borrowComponentLifecycleAdaptor();
        borrowed = component.borrowComponentLifecycleAdaptor();
        borrowed = component.borrowComponentLifecycleAdaptor();
        assertEquals(3, component.lifecycleAdapterPool.getNumActive());
        // assertEquals(3, pool.getMaxSize());

        // Should now grow
        borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);

        assertEquals(4, component.lifecycleAdapterPool.getNumActive());
    }

    public void testClearPool() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledJavaComponent component = new PooledJavaComponent(getObjectFactory(), pp);
        component.setService(getTestService());
        component.initialise();
        component.start();

        LifecycleAdapter borrowed = component.borrowComponentLifecycleAdaptor();
        assertEquals(1, component.lifecycleAdapterPool.getNumActive());
        component.returnComponentLifecycleAdaptor(borrowed);

        component.stop();
        component.start();
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());
    }

    public void testObjectUniqueness() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledJavaComponent component = new PooledJavaComponent(new PrototypeObjectFactory(UniqueComponent.class), pp);
        component.setService(getTestService());
        component.initialise();
        component.start();

        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        Object obj;

        obj = ((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get();
        assertNotNull(obj);
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
        String id1 = ((UniqueComponent) obj).getId();
        assertNotNull(id1);

        obj = ((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get();
        assertNotNull(obj);
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
        String id2 = ((UniqueComponent) obj).getId();
        assertNotNull(id2);

        obj = ((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get();
        assertNotNull(obj);
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
        String id3 = ((UniqueComponent) obj).getId();
        assertNotNull(id3);

        assertFalse("Service IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
        assertFalse("Service IDs " + id2 + " and " + id3 + " should be different", id2.equals(id3));
    }

    public void testDisposingFactoryDisposesObject() throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(new PrototypeObjectFactory(WaterMelon.class),
            getDefaultPoolingProfile());
        component.setService(getTestService());
        component.initialise();
        component.start();

        DefaultLifecycleAdapter lifecycleAdapter = (DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor();
        component.returnComponentLifecycleAdaptor(lifecycleAdapter);
        component.dispose();

        assertNull(lifecycleAdapter.componentObject.get());
    }

    public void testLifeCycleMethods() throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(new PrototypeObjectFactory(WaterMelon.class),
            getDefaultPoolingProfile());
        component.setService(getTestService());
        component.initialise();
        component.start();

        Object obj = component.lifecycleAdapterPool.getObjectFactory().getInstance();
        assertNotNull(obj);
        // assertTrue(of.validateObject(obj));
        // of.activateObject(obj);
        // of.passivateObject(obj);
        // of.destroyObject(obj);
    }

    private class Borrower extends Thread
    {
        private PooledJavaComponent component;
        private long time;

        public Borrower(PooledJavaComponent component, long time)
        {
            super("Borrower");
            if (component == null)
            {
                throw new IllegalArgumentException("Pool cannot be null");
            }
            this.component = component;
            if (time < 500)
            {
                time = 500;
            }
            this.time = time;
        }

        public void run()
        {
            try
            {
                LifecycleAdapter object = component.borrowComponentLifecycleAdaptor();
                try
                {
                    sleep(time);
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                component.returnComponentLifecycleAdaptor(object);
            }
            catch (Exception e)
            {
                fail("Borrower thread failed:\n" + ExceptionUtils.getStackTrace(e));
            }
        }

    }

}
