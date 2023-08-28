/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.provider;

import static org.mule.test.data.sample.extension.SampleDataExtension.adaptLegacy;

import org.mule.sdk.api.annotation.param.Config;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.test.data.sample.extension.SampleDataExtension;
import org.mule.test.data.sample.extension.SampleDataOperations;

public class ConfigAwareTestSampleDataProvider extends ConnectedTestSampleDataProvider {

  @org.mule.sdk.api.annotation.param.Config
  private SampleDataExtension config;

  @Override
  public Result<String, String> getSample() throws SampleDataException {
    return adaptLegacy(new SampleDataOperations().useConfig(config, connection, payload, attributes));
  }
}
