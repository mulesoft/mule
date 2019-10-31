/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.extension.api.annotation.metadata.RequiredForMetadata;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class MetadataConnectionProvider implements ConnectionProvider<MetadataConnection>, Startable {

  public static boolean STARTED = false;

  @Parameter
  @Optional
  @RequiredForMetadata
  String user;

  @Parameter
  @Optional
  String password;

  @Override
  public MetadataConnection connect() throws ConnectionException {
    return new MetadataConnection();
  }

  @Override
  public void disconnect(MetadataConnection metadataConnection) {

  }

  @Override
  public ConnectionValidationResult validate(MetadataConnection metadataConnection) {
    return ConnectionValidationResult.success();
  }

  @Override
  public void start() throws MuleException {
    STARTED = true;
  }
}
