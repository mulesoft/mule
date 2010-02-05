/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.pool;

import org.mule.api.object.ObjectFactory;
import org.mule.config.PoolingProfile;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.WaterMelon;

public class DefaultLifecycleEnabledObjectPoolTestCase extends AbstractPoolingTestCase
{
    public void testPoolStart() throws Exception
    {
        DefaultLifecycleEnabledObjectPool pool = createObjectPool();

        // pool was not started yet, objects must be uninitialized
        WaterMelon borrowed = borrow(pool);
        assertEquals("void", borrowed.getState());

        pool.start();
        assertEquals("started", borrowed.getState());
    }
    
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
            new DefaultLifecycleEnabledObjectPool(objectFactory, poolingProfile);
        
        pool.initialise();
        
        return pool;
    }

    private ObjectFactory createDefaultObjectFactory()
    {
        // WaterMelon implements some lifecycle methods
        return new PrototypeObjectFactory(WaterMelon.class);
    }
    
    private WaterMelon borrow(DefaultLifecycleEnabledObjectPool pool) throws Exception
    {
        return (WaterMelon) pool.borrowObject();
    }
}


