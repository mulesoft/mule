/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.data.sample.extension.SampleDataParameterGroup;
import org.mule.test.data.sample.extension.provider.GroupTestSampleDataProvider;

@Alias("parameter-group-listener")
@SampleData(GroupTestSampleDataProvider.class)
@MediaType(TEXT_PLAIN)
public class ParameterGroupListener extends TestSampleDataListener {

  @ParameterGroup(name = "group")
  private SampleDataParameterGroup group;
}
