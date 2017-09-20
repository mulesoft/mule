/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import static org.mule.test.classloading.ClassLoadingExtension.addClassLoader;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("none")
public class CLNoneConnectionProvider implements ConnectionProvider<String> {

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
