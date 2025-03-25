/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.connectivity.ConnectionProvider;
import org.mule.sdk.api.connectivity.ConnectionValidationResult;

import javax.inject.Inject;

public class JavaxInjectCompatibilityTestConnectionProvider
    implements ConnectionProvider<JavaxInjectCompatibilityTestConnection> {

  @Inject
  private ArtifactEncoding encoding;

  public JavaxInjectCompatibilityTestConnection connect() throws ConnectionException {
    return new JavaxInjectCompatibilityTestConnection(encoding);
  }

  public void disconnect(JavaxInjectCompatibilityTestConnection connection) {
    // nothing to do
  }

  public ConnectionValidationResult validate(JavaxInjectCompatibilityTestConnection connection) {
    return ConnectionValidationResult.success();
  }
}
