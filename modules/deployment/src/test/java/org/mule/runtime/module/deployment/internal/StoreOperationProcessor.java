/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.event.CoreEvent;

import javax.inject.Inject;

/**
 * Component used on deployment test that require policies to check that they are invoked
 * <p/>
 * Static state must be reset before each test is executed
 */
public class StoreOperationProcessor extends AbstractComponent implements org.mule.runtime.core.api.processor.Processor {

  @Inject
  ObjectStoreManager objectStoreManager;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    ObjectStore<String> myPartition =
        objectStoreManager.getOrCreateObjectStore("myPartition", ObjectStoreSettings.builder().persistent(true).build());
    String key = "value";
    if (myPartition.contains(key)) {
      myPartition.remove(key);
    }
    myPartition.store(key, key);
    return event;
  }
}
