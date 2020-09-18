/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tooling.extensions.metadata.internal.sampledata;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.tooling.extensions.metadata.internal.connection.TstExtensionClient;

public class ConfigLessSampleDataProvider implements SampleDataProvider {

  @Connection
  private TstExtensionClient client;

  @Override
  public String getId() {
    return getClass().getSimpleName();
  }

  @Override
  public Result<String, Void> getSample() throws SampleDataException {
    return Result.<String, Void>builder().output(client.getName()).build();
  }
}
