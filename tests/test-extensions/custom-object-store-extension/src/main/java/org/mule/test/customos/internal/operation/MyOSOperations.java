/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.customos.internal.operation;

import static org.mule.test.customos.internal.MyOSConnector.VALUES;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class MyOSOperations {

  @MediaType("*/*")
  public String retrieve(String key) throws ObjectStoreException {
    return VALUES.get(key).getValue();
  }

}
