/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import static org.mule.test.data.sample.extension.SampleDataExtension.adaptLegacy;

import org.mule.sdk.api.annotation.param.Connection;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.SampleDataConnection;

public class ConnectedTestSampleDataProvider extends ParameterizedTestSampleDataProvider {

  @Connection
  protected SampleDataConnection connection;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return adaptLegacy(connection.getResult(payload, attributes));
  }
}
