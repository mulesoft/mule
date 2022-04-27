/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal.operation;

import static org.mule.test.customos.internal.MyOSConnector.STORAGE;
import static org.mule.test.customos.internal.MyOSConnector.VALUES;

import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class MyOSOperations {

  @MediaType("*/*")
  public String retrieve(String key) throws ObjectStoreException {
    return VALUES.get(key).getValue();
  }

  public String retrieveFromSdkOS(String key) {
    return STORAGE.get(key).getValue();
  }
}
