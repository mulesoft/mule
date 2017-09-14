/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
