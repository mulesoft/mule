/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


