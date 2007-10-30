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

import org.mule.config.PoolingProfile;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.util.UUID;

public class PooledIdentifiableObjectFactoryTestCase extends PooledObjectFactoryTestCase
{
    //@Override
    protected UMOPooledObjectFactory createObjectFactory(Class clazz, PoolingProfile pp) throws Exception
    {
        UMOPooledObjectFactory of = new PooledIdentifiableObjectFactory(clazz, pp);
        of.initialise();        
        return of;
    }

    //@Override
    public void testLifeCycleMethods() throws Exception
    {
        UMOPooledObjectFactory of = createObjectFactory(Orange.class, getDefaultPoolingProfile());

        Identifiable obj = (Identifiable) ((PooledIdentifiableObjectFactory) of).makeObject(UUID.getUUID());
        assertNotNull(obj);
        assertTrue(((PooledIdentifiableObjectFactory) of).validateObject(obj.getId(), obj));
        ((PooledIdentifiableObjectFactory) of).activateObject(obj.getId(), obj);
        ((PooledIdentifiableObjectFactory) of).passivateObject(obj.getId(), obj);
        ((PooledIdentifiableObjectFactory) of).destroyObject(obj.getId(), obj);
    }    

    //@Override
    public void testLookupObject() throws Exception
    {
        UMOPooledObjectFactory of = createObjectFactory(Orange.class, getDefaultPoolingProfile());
        
        assertEquals(0, of.getPoolSize());

        Identifiable obj1 = (Identifiable) of.getOrCreate();
        assertNotNull(obj1);
        String id1 = obj1.getId();
        assertNotNull(id1);

        Identifiable obj2 = (Identifiable) of.getOrCreate();
        assertNotNull(obj2);
        String id2 = obj2.getId();
        assertNotNull(id2);

        assertFalse("Component IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
        
        Identifiable obj1A = (Identifiable) of.lookup(id1);
        assertNotNull(obj1A);
        assertEquals(id1, obj1A.getId());
        assertEquals(obj1, obj1A);
        
        Identifiable obj2A = (Identifiable) of.lookup(id2);
        assertNotNull(obj2A);
        assertEquals(id2, obj2A.getId());
        assertEquals(obj2, obj2A);
    }    
}
