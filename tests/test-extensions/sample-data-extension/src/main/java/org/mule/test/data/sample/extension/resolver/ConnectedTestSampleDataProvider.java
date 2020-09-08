/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.resolver;

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
