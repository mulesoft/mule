/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.data.sample.extension.SampleDataExtension;
import org.mule.test.data.sample.extension.provider.ConfigAwareTestSampleDataProvider;

@Alias("config-listener")
@SampleData(ConfigAwareTestSampleDataProvider.class)
@MediaType(TEXT_PLAIN)
public class ConfigTestSampleDataListener extends ConnectedTestSampleDataListener {

  @Config
  protected SampleDataExtension config;
}
