/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.source;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.data.sample.extension.SampleDataParameterGroup;
import org.mule.test.data.sample.extension.provider.GroupTestSampleDataProvider;

@Alias("parameter-group-listener")
@SampleData(GroupTestSampleDataProvider.class)
public class ParameterGroupListener extends TestSampleDataListener {

  @ParameterGroup(name = "group")
  private SampleDataParameterGroup group;
}
