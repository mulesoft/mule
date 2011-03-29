/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.store;

import org.mule.api.store.ListableObjectStore;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

public abstract class AbstractObjectStoreTestCase extends AbstractMuleTestCase
{
    ListableObjectStore store;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        if (store == null)
        {
            store = createStore();
            store.open();
        }
    }

    @Override
    protected void doTearDown() throws Exception
    {
        if (store != null)
        {
            store.close();
        }
        super.doTearDown();
    }

    protected abstract ListableObjectStore createStore();
    
    public void testStore() throws Exception
    {
        Apple apple = new Apple();
        apple.setBitten(true);
        assertNull(store.retrieve(1));
        store.store(1, apple);
        Object obj = store.retrieve(1);
        assertNotNull(obj);
        assertTrue(obj instanceof Apple);
        assertTrue(((Apple) obj).isBitten());
        assertEquals(apple, store.remove(1));
        assertNull(store.retrieve(1));
    }

    public void testListable() throws Exception
    {
        Apple apple = new Apple();
        store.store(1, apple);
        store.store(2, apple);
        store.store(3, apple);
        List keys = store.allKeys();
        assertNotNull(keys);
        assertEquals(3, keys.size());
        assertEquals(apple, store.remove(2));
        keys = store.allKeys();
        assertEquals(2, keys.size());
        store.store(4, new Orange());
        keys = store.allKeys();
        assertEquals(3, keys.size());
    }
}
