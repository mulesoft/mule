/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.resolver;

import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.SampleDataOperations;

public class GroupTestSampleDataProvider extends ConnectedTestSampleDataProvider {

  @Parameter
  private String groupParameter;

  @Parameter
  private String optionalParameter;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return new SampleDataOperations().useConnection(connection, groupParameter, optionalParameter);
  }
}
