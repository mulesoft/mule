/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import static org.mule.test.classloading.api.ClassLoadingHelper.addClassLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("none")
public class CLNoneConnectionProvider implements ClassLoadingConnectionProvider<String> {

  public static final String CONNECT = "Connect";
  public static final String DISCONNECT = "Disconnect";

  @Override
  public String connect() throws ConnectionException {
    addClassLoader(CONNECT + getKind());
    return "Conn";
  }

  @Override
  public void disconnect(String connection) {
    addClassLoader(DISCONNECT + getKind());
  }

  @Override
  public ConnectionValidationResult validate(String connection) {
    addClassLoader("Validate" + getKind());
    return ConnectionValidationResult.success();
  }

  public String getKind() {
    return "NONE";
  }
}
