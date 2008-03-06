/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.api.service.Service;
import org.mule.config.PoolingProfile;
import org.mule.model.seda.SedaModel;
import org.mule.model.seda.SedaService;
import org.mule.tck.services.UniqueComponent;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.util.ExceptionUtils;

public class PooledObjectFactoryTestCase extends AbstractObjectFactoryTestCase
{
    public static final byte MAX_ACTIVE = 3;
    public static final long MAX_WAIT = 1500;

    protected PoolingProfile getDefaultPoolingProfile()
    {
        PoolingProfile pp = new PoolingProfile();
        pp.setMaxActive(MAX_ACTIVE);
        pp.setMaxWait(MAX_WAIT);
        pp.setInitialisationPolicy(PoolingProfile.INITIALISE_NONE);
        return pp;
    }

    // @Override
    public ObjectFactory getObjectFactory()
    {
        return new PooledObjectFactory();
    }

    // @Override
    public void testGetObjectClass() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledObjectFactory factory = (PooledObjectFactory) getObjectFactory();
        factory.setObjectClass(Orange.class);
        factory.initialise();
        assertEquals(Orange.class, factory.getObjectClass());
    }

    // @Override
    public void testGet() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledObjectFactory factory = (PooledObjectFactory) getObjectFactory();
        factory.setObjectClass(Orange.class);
        factory.initialise();
        assertNotSame(factory.getInstance(), factory.getInstance());
    }

    public void testCreatePool() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, pp);
        of.initialise();

        assertEquals(0, of.getPoolSize());

        Object borrowed = of.getInstance();
        assertNotNull(borrowed);
        assertEquals(1, of.getPoolSize());
        of.release(borrowed);
        assertEquals(0, of.getPoolSize());

        borrowed = of.getInstance();
        assertNotNull(borrowed);
        assertEquals(1, of.getPoolSize());
        Object borrowed2 = of.getInstance();
        assertNotNull(borrowed2);
        assertEquals(2, of.getPoolSize());
    }

    public void testFailOnExhaust() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, pp);
        of.initialise();

        Object borrowed = null;

        for (int i = 0; i < MAX_ACTIVE; i++)
        {
            borrowed = of.getInstance();
            assertNotNull(borrowed);
            assertEquals(of.getPoolSize(), i + 1);
        }

        try
        {
            borrowed = of.getInstance();
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
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, pp);
        of.initialise();

        Object borrowed = null;

        assertEquals(0, of.getPoolSize());
        borrowed = of.getInstance();
        assertNotNull(borrowed);
        borrowed = of.getInstance();
        assertNotNull(borrowed);
        borrowed = of.getInstance();
        assertNotNull(borrowed);
        assertEquals(3, of.getPoolSize());

        // TODO
        // long starttime = System.currentTimeMillis();
        try
        {
            borrowed = of.getInstance();
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
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, pp);
        of.initialise();

        Object borrowed = null;

        assertEquals(0, of.getPoolSize());

        borrowed = of.getInstance();
        borrowed = of.getInstance();
        assertEquals(2, of.getPoolSize());

        // TODO
        // long starttime = System.currentTimeMillis();
        long borrowerWait = 500;
        Borrower borrower = new Borrower(of, borrowerWait);
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

        borrowed = of.getInstance();
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
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, pp);
        of.initialise();

        Object borrowed = of.getInstance();
        borrowed = of.getInstance();
        borrowed = of.getInstance();
        assertEquals(3, of.getPoolSize());
        // assertEquals(3, pool.getMaxSize());

        // Should now grow
        borrowed = of.getInstance();
        assertNotNull(borrowed);

        assertEquals(4, of.getPoolSize());
    }

    public void testClearPool() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, pp);
        of.initialise();

        Object borrowed = of.getInstance();
        assertEquals(1, of.getPoolSize());
        of.release(borrowed);

        of.dispose();
        assertEquals(0, of.getPoolSize());
    }

    public void testObjectUniqueness() throws Exception
    {
        PoolingProfile pp = getDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        PooledObjectFactory of = new PooledObjectFactory(UniqueComponent.class, pp);
        of.initialise();

        assertEquals(0, of.getPoolSize());

        Object obj;

        obj = of.getInstance();
        assertNotNull(obj);
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
        String id1 = ((UniqueComponent) obj).getId();
        assertNotNull(id1);

        obj = of.getInstance();
        assertNotNull(obj);
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
        String id2 = ((UniqueComponent) obj).getId();
        assertNotNull(id2);

        obj = of.getInstance();
        assertNotNull(obj);
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
        String id3 = ((UniqueComponent) obj).getId();
        assertNotNull(id3);

        assertFalse("Service IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
        assertFalse("Service IDs " + id2 + " and " + id3 + " should be different", id2.equals(id3));
    }

    public void testDisposingFactoryDisposesObject() throws Exception
    {
        PooledObjectFactory of = new PooledObjectFactory(WaterMelon.class, getDefaultPoolingProfile());
        of.initialise();

        WaterMelon wm = (WaterMelon) of.getInstance();
        of.release(wm);
        of.dispose();

        assertEquals("disposed", wm.getState());
    }

    public void testWithinComponent() throws Exception
    {
        SedaModel model = new SedaModel();
        model.setMuleContext(muleContext);
        muleContext.applyLifecycle(model);

        Service c = new SedaService();
        c.setName("test");
        PooledObjectFactory of = new PooledObjectFactory(Orange.class, getDefaultPoolingProfile());
        of.initialise();
        c.setComponentFactory(of);
        c.setModel(model);

        c.setMuleContext(muleContext);
        muleContext.applyLifecycle(c);

        assertTrue(c.getComponentFactory() instanceof PooledObjectFactory);
        assertEquals(0, ((PooledObjectFactory) c.getComponentFactory()).getPoolSize());
        assertTrue(c.getComponentFactory().getInstance() instanceof Orange);
        assertEquals(1, ((PooledObjectFactory) c.getComponentFactory()).getPoolSize());
        c.dispose();
        assertEquals(0, ((PooledObjectFactory) c.getComponentFactory()).getPoolSize());
    }

    public void testLifeCycleMethods() throws Exception
    {
        PooledObjectFactory of = new PooledObjectFactory(UniqueComponent.class, getDefaultPoolingProfile());
        of.initialise();

        Object obj = of.makeObject();
        assertNotNull(obj);
        assertTrue(of.validateObject(obj));
        of.activateObject(obj);
        of.passivateObject(obj);
        of.destroyObject(obj);
    }

    private class Borrower extends Thread
    {
        private PooledObjectFactory of;
        private long time;

        public Borrower(PooledObjectFactory of, long time)
        {
            super("Borrower");
            if (of == null)
            {
                throw new IllegalArgumentException("Pool cannot be null");
            }
            this.of = of;
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
                Object object = of.getInstance();
                try
                {
                    sleep(time);
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
                of.release(object);
            }
            catch (Exception e)
            {
                fail("Borrower thread failed:\n" + ExceptionUtils.getStackTrace(e));
            }
        }

    }

}
