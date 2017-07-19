/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.util.store;

import static org.junit.Assert.fail;
import org.mule.runtime.api.store.ObjectAlreadyExistsException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;

import junit.framework.Assert;
import org.junit.Test;

/**
 * This test verifies the contract described in {@link ObjectStore}
 */
public abstract class TemplateObjectStoreContractTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testContainsWithNullKey() {
    try {
      getObjectStore().contains(null);
      fail("contains() called with null key must throw ObjectStoreException");
    } catch (ObjectStoreException ose) {
      // this one was expected
    }
  }

  @Test
  public void testStoreWithNullKey() {
    try {
      Serializable value = getStorableValue();
      getObjectStore().store(null, value);
      fail("store() called with null key must throw ObjectStoreException");
    } catch (ObjectStoreException ose) {
      // this one was expected
    }
  }

  @Test
  public void testRetrieveWithNullKey() {
    try {
      getObjectStore().retrieve(null);
      fail("retrieve() called with null key must throw ObjectStoreException");
    } catch (ObjectStoreException ose) {
      // this one was expected
    }
  }

  @Test
  public void testRemoveWithNullKey() {
    try {
      getObjectStore().remove(null);
      fail("remove() called with null key must throw ObjectStoreException");
    } catch (ObjectStoreException ose) {
      // this one was expected
    }
  }

  @Test
  public void testRetrieveUnboundKey() throws ObjectStoreException {
    try {
      // nothing was stored in the OS yet so using any key must trigger the
      // ObjectDoesNotExistException
      String key = createKey();

      getObjectStore().retrieve(key);
      fail("retrieve() with unbound key must throw ObjectDoesNotExistException");
    } catch (ObjectDoesNotExistException odne) {
      // this one was expected
    }
  }

  @Test
  public void testRemoveWithUnboundKey() throws ObjectStoreException {
    try {
      // nothing was stored in the OS yet so using any key must trigger the
      // ObjectDoesNotExistException
      String key = createKey();

      getObjectStore().remove(key);
      fail("remove() with unbound key must throw ObjectDoesNotExistException");
    } catch (ObjectDoesNotExistException odnee) {
      // this one was expected
    }
  }

  @Test
  public void clear() throws ObjectStoreException {
    String key = this.createKey();
    Serializable value = this.getStorableValue();
    ObjectStore<Serializable> objectStore = this.getObjectStore();

    objectStore.store(key, value);
    Assert.assertTrue(objectStore.contains(key));

    objectStore.clear();

    Assert.assertFalse(objectStore.contains(key));

    // check it's still usable
    objectStore.store(key, value);
    Assert.assertTrue(objectStore.contains(key));
  }

  @Test
  public void testStoreWithExistingKey() throws ObjectStoreException {
    String key = createKey();
    Serializable value = getStorableValue();
    ObjectStore<Serializable> objectStore = getObjectStore();

    // storing for the first time must work
    objectStore.store(key, value);

    // storing with the same key again must fail
    try {
      objectStore.store(key, value);
      fail("store() with an existing key must throw ObjectAlreadyExistsException");
    } catch (ObjectAlreadyExistsException oaee) {
      // this one was expected
    }
  }

  protected String createKey() {
    return "theKey";
  }

  public abstract ObjectStore<Serializable> getObjectStore() throws ObjectStoreException;

  public abstract Serializable getStorableValue();
}
