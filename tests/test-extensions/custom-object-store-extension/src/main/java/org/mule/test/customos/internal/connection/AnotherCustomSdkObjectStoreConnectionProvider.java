/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.test.customos.internal.MyOSConnector.STORAGE;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.sdk.api.annotation.param.stereotype.Stereotype;

/**
 * Connection provider returning cached connection to the custom object store
 */
@Alias("another-custom-sdk-os-connection")
@Stereotype(CustomSdkObjectStoreConnectionStereotype.class)
public class AnotherCustomSdkObjectStoreConnectionProvider implements CachedConnectionProvider<CustomSdkObjectStoreManager> {


  @Override
  public CustomSdkObjectStoreManager connect() throws ConnectionException {
    STORAGE.clear();
    return new CustomSdkObjectStoreManager();
  }

  @Override
  public void disconnect(CustomSdkObjectStoreManager connection) {
    STORAGE.clear();
  }

  @Override
  public ConnectionValidationResult validate(CustomSdkObjectStoreManager connection) {
    return success();
  }
}
