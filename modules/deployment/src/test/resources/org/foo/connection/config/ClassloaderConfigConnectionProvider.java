/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.connection.config;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;

public class ClassloaderConfigConnectionProvider implements ConnectionProvider<ClassloaderConfigConnection>, Initialisable {

  private String fileContent;

  @Override
  public void initialise() throws InitialisationException {
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt");
    fileContent = IOUtils.toString(stream);
  }

  @Override
  public ClassloaderConfigConnection connect() {
    return new ClassloaderConfigConnection(fileContent);
  }

  @Override
  public void disconnect(ClassloaderConfigConnection connection) {
    connection.invalidate();
  }

  @Override
  public ConnectionValidationResult validate(ClassloaderConfigConnection connection) {
    return null;
  }
}