/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.store;

import static org.junit.Assert.fail;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.SimpleMemoryObjectStore;
import org.mule.tck.core.util.store.TemplateObjectStoreContractTestCase;
import org.mule.tck.testmodels.fruit.Banana;

import java.io.Serializable;

import org.junit.Test;

public class SimpleMemoryObjectStoreContractTestCase extends TemplateObjectStoreContractTestCase {

  @Override
  public ObjectStore<Serializable> getObjectStore() {
    return new SimpleMemoryObjectStore<>();
  }

  @Override
  public Serializable getStorableValue() {
    return new Banana();
  }

  @Test
  public void storesNullValue() throws Exception {
    try {
      getObjectStore().store("key", null);
      fail("store() called with null value must throw ObjectStoreException");
    } catch (ObjectStoreException ose) {
      // this one was expected
    }
  }
}
