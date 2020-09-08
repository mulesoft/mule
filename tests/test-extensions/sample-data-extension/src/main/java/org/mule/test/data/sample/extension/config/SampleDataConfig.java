/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.data.sample.extension.SampleDataOperations;
import org.mule.test.data.sample.extension.connection.SampleDataConnectionProvider;

@Configuration
@ConnectionProviders(SampleDataConnectionProvider.class)
@Operations(SampleDataOperations.class)
//@Sources({SourceWithConfiguration.class, SourceWithConnection.class, SourceWithValuesWithRequiredParameters.class,
//    SourceWithValuesWithRequiredParameterInsideParamGroup.class,
//    SourceWithRequiredParameterWithAlias.class, SourceWithRequiredParameterInsideShowInDslGroup.class,
//    SourceWithMultiLevelValue.class, SourceMustNotStart.class})
public class SampleDataConfig {

  @Parameter
  private String prefix;

  public String getPrefix() {
    return prefix;
  }
}
