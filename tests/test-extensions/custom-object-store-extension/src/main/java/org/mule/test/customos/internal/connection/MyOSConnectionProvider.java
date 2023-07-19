/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
