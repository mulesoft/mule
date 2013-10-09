/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.pool;

import org.mule.api.object.ObjectFactory;
import org.mule.config.PoolingProfile;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.WaterMelon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultLifecycleEnabledObjectPoolTestCase extends AbstractPoolingTestCase
{

    @Test
    public void testPoolStart() throws Exception
    {
        DefaultLifecycleEnabledObjectPool pool = createObjectPool();

        // pool was not started yet, objects must be uninitialized
        WaterMelon borrowed = borrow(pool);
        assertEquals("void", borrowed.getState());

        pool.start();
        assertEquals("started", borrowed.getState());
    }
    
    @Test
    public void testPoolStop() throws Exception
    {
        DefaultLifecycleEnabledObjectPool pool = createObjectPool();
        pool.start();
        
        WaterMelon borrowed = borrow(pool);
        
        pool.stop();
        assertEquals("stopped", borrowed.getState());
    }

    private DefaultLifecycleEnabledObjectPool createObjectPool() throws Exception
    {
        PoolingProfile poolingProfile = createDefaultPoolingProfile();
        ObjectFactory objectFactory = createDefaultObjectFactory();
        DefaultLifecycleEnabledObjectPool pool =
            new DefaultLifecycleEnabledObjectPool(objectFactory, poolingProfile, muleContext);
        
        pool.initialise();
        
        return pool;
    }

    private ObjectFactory createDefaultObjectFactory()
    {
        // WaterMelon implements some lifecycle methods
        PrototypeObjectFactory factory = new PrototypeObjectFactory(WaterMelon.class);
        return factory;
    }
    
    private WaterMelon borrow(DefaultLifecycleEnabledObjectPool pool) throws Exception
    {
        return (WaterMelon) pool.borrowObject();
    }
}


