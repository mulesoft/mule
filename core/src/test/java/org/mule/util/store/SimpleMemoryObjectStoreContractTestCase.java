/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
