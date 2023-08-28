/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import javax.inject.Inject;

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
