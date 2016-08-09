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

public class MetadataConnectionProvider implements ConnectionProvider<MetadataConnection> {

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
}
