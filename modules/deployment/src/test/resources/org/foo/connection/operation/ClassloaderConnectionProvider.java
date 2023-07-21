/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.connection.operation;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;

import java.io.InputStream;

public class ClassloaderConnectionProvider implements ConnectionProvider<ClassloaderConnection>, Initialisable {

  @Override
  public void initialise() throws InitialisationException {
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt");
  }

  @Override
  public ClassloaderConnection connect() {
    return new ClassloaderConnection();
  }

  @Override
  public void disconnect(ClassloaderConnection connection) {
    connection.invalidate();
  }

  @Override
  public ConnectionValidationResult validate(ClassloaderConnection connection) {
    return null;
  }
}