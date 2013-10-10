/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
            getObjectStore().retrieve("this_key_does_not_exist");
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
            getObjectStore().remove("this_key_does_not_exist");
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
        String key = "theKey";
        Serializable value = getStorableValue();
        ObjectStore<Serializable> objectStore = getObjectStore();
        
        // storing for the first time must work
        objectStore.store(key, value);
        
        // storing with the same key again must fail
        try
        {
            objectStore.store(key, value);
            fail("store() with and existing key must throw ObjectAlreadyExistsException");
        }
        catch (ObjectAlreadyExistsException oaee)
        {
            // this one was expected
        }
    }
    
    public abstract ObjectStore<Serializable> getObjectStore();
    
    public abstract Serializable getStorableValue();
}
