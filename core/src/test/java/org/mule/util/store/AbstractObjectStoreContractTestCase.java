/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * This test verifies the contract described in {@link ObjectStore}
 */
public abstract class AbstractObjectStoreContractTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testContainsWithNullKey()
    {
        try
        {
            getObjectStore().contains(null);
            fail("contains() called with null key must throw ObjectStoreException");
        }
        catch (ObjectStoreException ose)
        {
            // this one was expected
        }
    }

    @Test
    public void testStoreWithNullKey()
    {
        try
        {
            Serializable value = getStorableValue();
            getObjectStore().store(null, value);
            fail("store() called with null key must throw ObjectStoreException");
        }
        catch (ObjectStoreException ose)
        {
            // this one was expected
        }
    }

    @Test
    public void testRetrieveWithNullKey()
    {
        try
        {
            getObjectStore().retrieve(null);
            fail("retrieve() called with null key must throw ObjectStoreException");
        }
        catch (ObjectStoreException ose)
        {
            // this one was expected
        }
    }

    @Test
    public void testRemoveWithNullKey()
    {
        try
        {
            getObjectStore().remove(null);
            fail("remove() called with null key must throw ObjectStoreException");
        }
        catch (ObjectStoreException ose)
        {
            // this one was expected
        }
    }

    @Test
    public void testRetrieveUnboundKey() throws ObjectStoreException
    {
        try
        {
            // nothing was stored in the OS yet so using any key must trigger the ObjectDoesNotExistException
            Serializable key = createKey();

            getObjectStore().retrieve(key);
            fail("retrieve() with unbound key must throw ObjectDoesNotExistException");
        }
        catch (ObjectDoesNotExistException odne)
        {
            // this one was expected
        }
    }

    @Test
    public void testRemoveWithUnboundKey() throws ObjectStoreException
    {
        try
        {
            // nothing was stored in the OS yet so using any key must trigger the ObjectDoesNotExistException
            Serializable key = createKey();

            getObjectStore().remove(key);
            fail("remove() with unbound key must throw ObjectDoesNotExistException");
        }
        catch (ObjectDoesNotExistException odnee)
        {
            // this one was expected
        }
    }

    @Test
    public void testStoreWithExistingKey() throws ObjectStoreException
    {
        Serializable key = createKey();
        Serializable value = getStorableValue();
        ObjectStore<Serializable> objectStore = getObjectStore();

        // storing for the first time must work
        objectStore.store(key, value);

        // storing with the same key again must fail
        try
        {
            objectStore.store(key, value);
            fail("store() with an existing key must throw ObjectAlreadyExistsException");
        }
        catch (ObjectAlreadyExistsException oaee)
        {
            // this one was expected
        }
    }

    protected Serializable createKey()
    {
        return "theKey";
    }

    public abstract ObjectStore<Serializable> getObjectStore() throws ObjectStoreException;

    public abstract Serializable getStorableValue();
}
