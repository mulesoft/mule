/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
