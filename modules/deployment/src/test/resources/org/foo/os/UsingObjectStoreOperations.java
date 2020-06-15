/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.os;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;

import java.io.Serializable;
import java.lang.RuntimeException;

import javax.inject.Inject;

public class UsingObjectStoreOperations {
  
  @Inject
  private ObjectStoreManager objectStoreManager;

  public void useObjectStore() {
    
    try {
      ObjectStore<Serializable> os = objectStoreManager.getOrCreateObjectStore("osName", ObjectStoreSettings.builder().persistent(true).build());
      os.store("valueKey", new UsingObjectStoreData("Hello World!"));
      os.retrieve("valueKey");
    } catch (ObjectStoreException ose) {
      throw new RuntimeException(ose);
    }
  }
}
