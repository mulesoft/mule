/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.customos.internal.connection;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.test.customos.internal.MyOSConnector.VALUES;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;

/**
 * This is a Connection Provider, is executed to obtain new connections when an operation or message source requires.;
 */
@Stereotype(ObjectStoreConnectionStereotype.class)
public class MyOSConnectionProvider implements CachedConnectionProvider<MyOSConnection> {


  public MyOSConnection connect() throws ConnectionException {
    VALUES.clear();
    return new MyOSConnection();
  }

  public void disconnect(MyOSConnection connection) {
    VALUES.clear();
  }

  public ConnectionValidationResult validate(MyOSConnection connection) {
    return success();
  }
}
