/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.testmodels.fruit.Banana;

import java.io.Serializable;

import static org.junit.Assert.fail;

public class SimpleMemoryObjectStoreContractTestCase extends AbstractObjectStoreContractTestCase
{
    @Override
    public ObjectStore<Serializable> getObjectStore()
    {
        return new SimpleMemoryObjectStore<Serializable>();
    }

    @Override
    public Serializable getStorableValue()
    {
        return new Banana();
    }

    public void testStoreNullValue() throws Exception
    {
        try
        {
            getObjectStore().store("key", null);
            fail("store() called with null value must throw ObjectStoreException");
        }
        catch (ObjectStoreException ose)
        {
            // this one was expected
        }
    }
}
