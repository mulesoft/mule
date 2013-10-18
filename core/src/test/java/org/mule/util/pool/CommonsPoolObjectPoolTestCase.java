/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.pool;

import org.mule.api.MuleContext;
import org.mule.api.object.ObjectFactory;
import org.mule.config.PoolingProfile;
import org.mule.tck.testmodels.fruit.BananaFactory;

import java.util.NoSuchElementException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommonsPoolObjectPoolTestCase extends AbstractPoolingTestCase
{
    @Test
    public void testPoolExhaustedFail() throws Exception
    {
        ObjectPool pool = createPoolWithExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_FAIL);
        assertEquals(0, pool.getNumActive());
                
        borrowObjectsUntilPoolIsFull(pool);
        
        // borrow one more, this must fail
        try
        {
            pool.borrowObject();
            fail("borrowing an object from a pool with policy WHEN_EXHAUSTED_FAIL must fail");
        }
        catch (NoSuchElementException nse)
        {
            // this one was expected
        }
    }
    
    @Test
    public void testPoolExhaustedGrow() throws Exception
    {
        ObjectPool pool = createPoolWithExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_GROW);
        assertEquals(0, pool.getNumActive());
        
        borrowObjectsUntilPoolIsFull(pool);
        
        // borrow one more, this must make the pool grow
        pool.borrowObject();
        assertEquals(MAX_ACTIVE + 1, pool.getNumActive());
    }
    
    @Test
    public void testPoolExhaustedWait() throws Exception
    {
        ObjectPool pool = createPoolWithExhaustedAction(PoolingProfile.WHEN_EXHAUSTED_WAIT);
        assertEquals(0, pool.getNumActive());
        
        borrowObjectsUntilPoolIsFull(pool);
        
        // borrow one more, this must make the pool grow
        long before = System.currentTimeMillis();
        try
        {
            pool.borrowObject();
            fail("WHEN_EXHAUSTED_WAIT was specified but the pool returned an object");
        }
        catch (NoSuchElementException nse)
        {
            long delta = System.currentTimeMillis() - before;
            assertTrue(delta >= MAX_WAIT);
        }
    }
    
    @Test
    public void testInitPoolNone() throws Exception
    {
        ObjectPool pool = createPoolWithInitialisationPolicy(PoolingProfile.INITIALISE_NONE);
        CountingObjectFactory objectFactory = (CountingObjectFactory) pool.getObjectFactory();
        assertEquals(0, objectFactory.getInstanceCount());
    }
    
    @Test
    public void testInitPoolOne() throws Exception
    {
        ObjectPool pool = createPoolWithInitialisationPolicy(PoolingProfile.INITIALISE_ONE);
        CountingObjectFactory objectFactory = (CountingObjectFactory) pool.getObjectFactory();
        assertEquals(1, objectFactory.getInstanceCount());
    }
    
    @Test
    public void testInitPoolAll() throws Exception
    {   
        ObjectPool pool = createPoolWithInitialisationPolicy(PoolingProfile.INITIALISE_ALL);
        CountingObjectFactory objectFactory = (CountingObjectFactory) pool.getObjectFactory();
        assertEquals(MAX_ACTIVE, objectFactory.getInstanceCount());
    }

    private ObjectPool createPoolWithExhaustedAction(int exhaustedAction) throws Exception
    {
        PoolingProfile poolingProfile = createDefaultPoolingProfile();
        poolingProfile.setExhaustedAction(exhaustedAction);

        ObjectFactory objectFactory = new BananaFactory();
        return createPool(poolingProfile, objectFactory);
    }

    private ObjectPool createPoolWithInitialisationPolicy(int initPolicy) throws Exception
    {
        PoolingProfile poolingProfile = createDefaultPoolingProfile();
        poolingProfile.setInitialisationPolicy(initPolicy);
        
        ObjectFactory objectFactory = new CountingObjectFactory();
        return createPool(poolingProfile, objectFactory);
    }
    
    private ObjectPool createPool(PoolingProfile poolingProfile, ObjectFactory objectFactory) throws Exception
    {        
        CommonsPoolObjectPool pool = new CommonsPoolObjectPool(objectFactory, poolingProfile, muleContext);
        pool.initialise();
        return pool;
    }

    private void borrowObjectsUntilPoolIsFull(ObjectPool pool) throws Exception
    {
        for (int i = 1; i <= MAX_ACTIVE; i++)
        {
            pool.borrowObject();
            assertEquals(i, pool.getNumActive());
        }
    }
    
    private static class CountingObjectFactory extends BananaFactory
    {
        private int instanceCount = 0;
        
        @Override
        public Object getInstance(MuleContext muleContext) throws Exception
        {
            instanceCount++;
            return super.getInstance(muleContext);
        }

        public int getInstanceCount()
        {
            return instanceCount;
        }
    }
}
