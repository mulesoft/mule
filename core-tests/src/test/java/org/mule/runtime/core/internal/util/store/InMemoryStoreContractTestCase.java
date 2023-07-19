/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.store;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.tck.core.util.store.TemplateObjectStoreContractTestCase;
import org.mule.tck.core.util.store.InMemoryObjectStore;

import java.io.Serializable;

public class InMemoryStoreContractTestCase extends TemplateObjectStoreContractTestCase {

  @Override
  public ObjectStore<Serializable> getObjectStore() {
    return new InMemoryObjectStore<>();
  }

  @Override
  public Serializable getStorableValue() {
    return null;
  }
}
