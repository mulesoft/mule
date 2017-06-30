/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.test.values.extension.resolver.MultiLevelValuesProvider;
import org.mule.test.values.extension.resolver.SimpleValuesProvider;
import org.mule.test.values.extension.resolver.WithConfigValuesProvider;
import org.mule.test.values.extension.resolver.WithConnectionValuesProvider;
import org.mule.test.values.extension.resolver.WithMuleContextValuesProvider;
import org.mule.test.values.extension.resolver.WithRequiredParameterFromGroupValuesProvider;
import org.mule.test.values.extension.resolver.WithRequiredParametersValuesProvider;

import java.util.List;

public class ValuesOperations {

  public void singleValuesEnabledParameter(@OfValues(SimpleValuesProvider.class) String channels) {

  }

  public void singleValuesEnabledParameterWithConnection(@OfValues(WithConnectionValuesProvider.class) String channels,
                                                         @Connection ValuesConnection connection) {}

  public void singleValuesEnabledParameterWithConfiguration(@OfValues(WithConfigValuesProvider.class) String channels,
                                                            @Connection ValuesConnection connection) {}

  public void singleValuesEnabledParameterWithRequiredParameters(@OfValues(WithRequiredParametersValuesProvider.class) String channels,
                                                                 String requiredString,
                                                                 boolean requiredBoolean,
                                                                 int requiredInteger,
                                                                 List<String> strings) {}

  public void singleValuesEnabledParameterInsideParameterGroup(@ParameterGroup(
      name = "ValuesGroup") GroupWithValuesParameter optionsParameter) {}

  public void singleValuesEnabledParameterRequiresValuesOfParameterGroup(@OfValues(WithRequiredParameterFromGroupValuesProvider.class) String values,
                                                                         @ParameterGroup(
                                                                             name = "ValuesGroup") GroupWithValuesParameter optionsParameter) {}

  public void multiLevelValue(@OfValues(MultiLevelValuesProvider.class) @ParameterGroup(
      name = "values") GroupAsMultiLevelValue optionsParameter) {

  }

  public void singleValuesWithRequiredParameterWithAlias(@ParameterGroup(
      name = "someGroup") WithRequiredParameterWithAliasGroup group) {}

  public void resolverGetsMuleContextInjection(@OfValues(WithMuleContextValuesProvider.class) String channel) {

  }

  public void valuesInsideShowInDslGroup(@OfValues(WithRequiredParameterFromGroupValuesProvider.class) String values,
                                         @ParameterGroup(name = "ValuesGroup",
                                             showInDsl = true) GroupWithValuesParameter optionsParameter) {

  }
}
