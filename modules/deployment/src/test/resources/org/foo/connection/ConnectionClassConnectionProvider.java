/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.connection;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;

public class ConnectionClassConnectionProvider implements ConnectionProvider<ClassConnection123>, Initialisable {

  @Override
  public void initialise() throws InitialisationException {
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt");
  }

  @Override
  public ClassConnection123 connect() {
    return new ClassConnection123();
  }

  @Override
  public void disconnect(ClassConnection123 connection) {
    connection.invalidate();
  }

  @Override
  public ConnectionValidationResult validate(ClassConnection123 connection) {
    return null;
  }
}