/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.api.component.LifecycleAdapter;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.config.PoolingProfile;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.services.UniqueComponent;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.util.ExceptionUtils;
import org.mule.util.pool.AbstractPoolingTestCase;

import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PooledJavaComponentTestCase extends AbstractPoolingTestCase
{    
    @Test
    public void testComponentCreation() throws Exception
    {
        PrototypeObjectFactory objectFactory = getDefaultObjectFactory();

        PoolingProfile pp = createDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);

        PooledJavaComponent component = new PooledJavaComponent(objectFactory, pp);
        component.setMuleContext(muleContext);
        assertNotNull(component.getObjectFactory());
        assertEquals(objectFactory, component.getObjectFactory());
        assertEquals(Orange.class, component.getObjectType());

        assertNotNull(component.getPoolingProfile());
        assertEquals(pp, component.getPoolingProfile());
    }

    @Test
    public void testPoolManagement() throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(getDefaultObjectFactory(), createDefaultPoolingProfile());
        assertNull(component.lifecycleAdapterPool);
        
        component.setFlowConstruct(getTestService());
        component.setMuleContext(muleContext);
        component.initialise();
        assertNull(component.lifecycleAdapterPool);
        
        component.start();
        assertNotNull(component.lifecycleAdapterPool);
        
        component.stop();
        assertNull(component.lifecycleAdapterPool);
    }

    @Test
    public void testStartStop() throws Exception
    {
        PooledJavaComponent component = createPooledComponent();
        assertNotSame(component.borrowComponentLifecycleAdaptor(), component.borrowComponentLifecycleAdaptor());
        
        component.stop();
        component.start();

        Object la1 = ((DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject;
        Object la2 = ((DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject;
        assertNotSame(la1, la2);
    }

    @Test
    public void testCreateLifecycleAdapters() throws Exception
    {
        PooledJavaComponent component = createPooledComponent();
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

    @Test
    public void testFailOnExhaust() throws Exception
    {
        PoolingProfile pp = createDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        
        PooledJavaComponent component = createPooledComponent(pp);
        borrowLifecycleAdaptersUntilPoolIsFull(component);

        try
        {
            component.borrowComponentLifecycleAdaptor();
            fail("Should throw an Exception");
        }
        catch (NoSuchElementException nse)
        {
            // expected
        }
    }

    @Test
    public void testBlockExpiryOnExhaust() throws Exception
    {
        PoolingProfile pp = createDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        
        PooledJavaComponent component = createPooledComponent(pp);
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());
        
        borrowLifecycleAdaptersUntilPoolIsFull(component);

        long startTime = System.currentTimeMillis();
        try
        {
            component.borrowComponentLifecycleAdaptor();
            fail("Should throw an Exception");
        }
        catch (NoSuchElementException e)
        {
            long totalTime = System.currentTimeMillis() - startTime;
            assertTrue(totalTime >= MAX_WAIT);
        }
    }

    @Test
    public void testBlockOnExhaust() throws Exception
    {
        PoolingProfile pp = createDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        
        PooledJavaComponent component = createPooledComponent(pp);
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        // borrow all but one lifecycle adapters
        int oneRemainingInPool = (MAX_ACTIVE - 1);
        for (int i = 0; i < oneRemainingInPool; i++)
        {
            LifecycleAdapter borrowed = component.borrowComponentLifecycleAdaptor();
            assertNotNull(borrowed);
            assertEquals(component.lifecycleAdapterPool.getNumActive(), i + 1);
        }
        assertEquals(oneRemainingInPool, component.lifecycleAdapterPool.getNumActive());
        
        long startTime = System.currentTimeMillis();
        int borrowerWait = 500;
        Borrower borrower = new Borrower(component, borrowerWait);
        new Thread(borrower, "BorrowThread").start();

        // Make sure the borrower borrows first
        try
        {
            Thread.sleep(200);
        }
        catch (InterruptedException e)
        {
            // ignore
        }

        // this will get an object from the pool eventually, after Borrower has returned it
        Object borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        long totalTime = System.currentTimeMillis() - startTime;
        assertTrue(totalTime >= borrowerWait);
    }
    
    @Test
    public void testGrowOnExhaust() throws Exception
    {
        PoolingProfile pp = createDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_GROW);
        
        PooledJavaComponent component = createPooledComponent(pp);

        borrowLifecycleAdaptersUntilPoolIsFull(component);

        // Should now grow
        Object borrowed = component.borrowComponentLifecycleAdaptor();
        assertNotNull(borrowed);
        assertEquals(MAX_ACTIVE + 1, component.lifecycleAdapterPool.getNumActive());
    }

    @Test
    public void testClearPool() throws Exception
    {
        PoolingProfile pp = createDefaultPoolingProfile();
        pp.setExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        
        PooledJavaComponent component = createPooledComponent(pp);

        LifecycleAdapter borrowed = component.borrowComponentLifecycleAdaptor();
        assertEquals(1, component.lifecycleAdapterPool.getNumActive());
        component.returnComponentLifecycleAdaptor(borrowed);
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        component.stop();
        component.start();
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());
    }

    @Test
    public void testObjectUniqueness() throws Exception
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(UniqueComponent.class);
        PooledJavaComponent component = createPooledComponent(objectFactory);
        assertEquals(0, component.lifecycleAdapterPool.getNumActive());

        String id1 = getIdFromObjectCreatedByPool(component);
        String id2 = getIdFromObjectCreatedByPool(component);
        String id3 = getIdFromObjectCreatedByPool(component);

        assertFalse("Service IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
        assertFalse("Service IDs " + id2 + " and " + id3 + " should be different", id2.equals(id3));
        assertFalse("Service IDs " + id1 + " and " + id3 + " should be different", id1.equals(id3));
    }
    
    @Test
    public void testDisposingFactoryDisposesObject() throws Exception
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(WaterMelon.class);
        PooledJavaComponent component = createPooledComponent(objectFactory);

        DefaultComponentLifecycleAdapter lifecycleAdapter = (DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor();
        component.returnComponentLifecycleAdaptor(lifecycleAdapter);
        component.stop();
        component.dispose();

        assertNull(lifecycleAdapter.componentObject);
    }
    
    private PrototypeObjectFactory getDefaultObjectFactory() throws InitialisationException
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(Orange.class);
        objectFactory.initialise();
        return objectFactory;
    }
    
    private PooledJavaComponent createPooledComponent() throws Exception
    {
        return createPooledComponent(createDefaultPoolingProfile(), getDefaultObjectFactory());
    }

    private PooledJavaComponent createPooledComponent(ObjectFactory objectFactory) throws Exception
    {
        return createPooledComponent(createDefaultPoolingProfile(), objectFactory);
    }
    
    private PooledJavaComponent createPooledComponent(PoolingProfile poolingProfile) throws Exception
    {
        return createPooledComponent(poolingProfile, getDefaultObjectFactory());
    }
    
    private PooledJavaComponent createPooledComponent(PoolingProfile poolingProfile, ObjectFactory objectFactory) throws Exception
    {
        PooledJavaComponent component = new PooledJavaComponent(objectFactory, poolingProfile);
        component.setMuleContext(muleContext);
        component.setFlowConstruct(getTestService());
        component.initialise();
        component.start();
        return component;
    }
    
    private void borrowLifecycleAdaptersUntilPoolIsFull(PooledJavaComponent component) throws Exception
    {
        for (int i = 0; i < MAX_ACTIVE; i++)
        {
            Object borrowed = component.borrowComponentLifecycleAdaptor();
            assertNotNull(borrowed);
            assertEquals(component.lifecycleAdapterPool.getNumActive(), i + 1);
        }
        assertEquals(MAX_ACTIVE, component.lifecycleAdapterPool.getNumActive());
    }

    private String getIdFromObjectCreatedByPool(PooledJavaComponent component) throws Exception
    {
        DefaultComponentLifecycleAdapter lifecycleAdapter = 
            (DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor();
        Object obj = lifecycleAdapter.componentObject;
        
        // there is a slight chance that GC kicks in before we can get a hard reference to the 
        // object. If this occurs, talk do Dirk and Andrew P about how to fix this
        assertNotNull(obj);
        
        assertTrue("Object should be of type UniqueComponent", obj instanceof UniqueComponent);
     
        String id = ((UniqueComponent) obj).getId();
        assertNotNull(id);
        return id;
    }
    
    private static class Borrower implements Runnable
    {
        private PooledJavaComponent component;
        private long time;

        public Borrower(PooledJavaComponent component, long time)
        {
            super();
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
                    Thread.sleep(time);
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
