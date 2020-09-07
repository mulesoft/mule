/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.resolver;

import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.SampleDataOperations;
import org.mule.test.data.sample.extension.config.SampleDataConfig;

public class ConfigAwareTestSampleDataProvider extends ConnectedTestSampleDataProvider {

  @Config
  private SampleDataConfig config;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return new SampleDataOperations().useConfig(config, connection, payload, attributes);
  }
}
