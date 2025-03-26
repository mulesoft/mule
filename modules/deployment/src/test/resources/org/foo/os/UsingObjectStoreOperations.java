/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.os;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.store.ObjectDoesNotExistException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.store.ObjectStoreSettings;
import org.mule.runtime.core.api.util.UUID;

import java.io.Serializable;

import jakarta.inject.Inject;

public class UsingObjectStoreOperations implements Initialisable {
  
  @Inject
  private ObjectStoreManager objectStoreManager;

  private ObjectStore<Serializable> os;

  public void initialise() {
    os = objectStoreManager.getOrCreateObjectStore("osName" + UUID.getUUID(), ObjectStoreSettings.builder().persistent(true).build());
  }

  public void useObjectStore() {
    try {
      try {
        os.remove("valueKey");
      } catch (ObjectDoesNotExistException ose) {}
      os.store("valueKey", new UsingObjectStoreData("Hello World!"));
      os.retrieve("valueKey");
    } catch (ObjectStoreException ose) {
      throw new RuntimeException(ose);
    }
  }
}
