/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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