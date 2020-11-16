/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.MultiLevelValueProvider;
import org.mule.test.values.extension.resolver.SimpleValueProvider;
import org.mule.test.values.extension.resolver.WithComplexActingParameter;
import org.mule.test.values.extension.resolver.WithConnectionValueProvider;
import org.mule.test.values.extension.resolver.WithConfigValueProvider;
import org.mule.test.values.extension.resolver.WithErrorValueProvider;
import org.mule.test.values.extension.resolver.WithOptionalParametersValueProvider;
import org.mule.test.values.extension.resolver.WithOptionalParametersWithDefaultValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredAndOptionalParametersValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredParameterFromGroupValueProvider;
import org.mule.test.values.extension.resolver.WithRequiredParametersValueProvider;
import org.mule.test.values.extension.resolver.WithMuleContextValueProvider;

import java.util.List;

public class ValuesOperations {

  public void singleValuesEnabledParameter(@OfValues(SimpleValueProvider.class) String channels) {

  }

  public void singleValuesEnabledParameterWithConnection(@OfValues(WithConnectionValueProvider.class) String channels,
                                                         @Connection ValuesConnection connection) {}

  public void singleValuesEnabledParameterWithConfiguration(@OfValues(WithConfigValueProvider.class) String channels,
                                                            @Connection ValuesConnection connection) {}

  public void singleValuesEnabledParameterWithRequiredParameters(@OfValues(WithRequiredParametersValueProvider.class) String channels,
                                                                 String requiredString,
                                                                 boolean requiredBoolean,
                                                                 int requiredInteger,
                                                                 List<String> strings) {}

  public void singleValuesEnabledParameterInsideParameterGroup(@ParameterGroup(
      name = "ValuesGroup") GroupWithValuesParameter optionsParameter) {}

  public void singleValuesEnabledParameterRequiresValuesOfParameterGroup(@OfValues(WithRequiredParameterFromGroupValueProvider.class) String values,
                                                                         @ParameterGroup(
                                                                             name = "ValuesGroup") GroupWithValuesParameter optionsParameter) {}

  public void multiLevelValue(@OfValues(MultiLevelValueProvider.class) @ParameterGroup(
      name = "values") GroupAsMultiLevelValue optionsParameter) {

  }

  public void singleValuesWithRequiredParameterWithAlias(@ParameterGroup(
      name = "someGroup") WithRequiredParameterWithAliasGroup group) {}

  public void resolverGetsMuleContextInjection(@OfValues(WithMuleContextValueProvider.class) String channel) {

  }

  public void valuesInsideShowInDslGroup(@OfValues(WithRequiredParameterFromGroupValueProvider.class) String values,
                                         @ParameterGroup(name = "ValuesGroup",
                                             showInDsl = true) GroupWithValuesParameter optionsParameter) {

  }

  public void withErrorValueProvider(@OfValues(WithErrorValueProvider.class) String values, String errorCode) {

  }

  public void withComplexActingParameter(@Optional @OfValues(WithComplexActingParameter.class) String providedParameter,
                                         ComplexActingParameter complexActingParameter) {}

  public void withRequiredParameterAndOptionalParameterAsRequired(@OfValues(WithRequiredAndOptionalParametersValueProvider.class) String providedParameters,
                                                                  String requiredValue, String optionalValue) {}

  public void withRequiredAndOptionalParameters(@OfValues(WithRequiredAndOptionalParametersValueProvider.class) String providedParameters,
                                                String requiredValue, @Optional String optionalValue) {}

  public void withRequiredAndOptionalWithDefaultParameters(@OfValues(WithRequiredAndOptionalParametersValueProvider.class) String providedParameters,
                                                           String requiredValue, @Optional(
                                                               defaultValue = "OPERATION_DEFAULT_VALUE") String optionalValue) {}

  public void withOptionalParameterAsRequired(@OfValues(WithOptionalParametersValueProvider.class) String providedParameters,
                                              String optionalValue) {}

  public void withOptionalParameter(@OfValues(WithOptionalParametersValueProvider.class) String providedParameters,
                                    @Optional String optionalValue) {}

  public void withOptionalParameterWithDefault(@OfValues(WithOptionalParametersValueProvider.class) String providedParameters,
                                               @Optional(defaultValue = "OPERATION_DEFAULT_VALUE") String optionalValue) {}

  public void withVPOptionalParameterWithDefaultValue(@OfValues(WithOptionalParametersWithDefaultValueProvider.class) String providedParameters,
                                                      @Optional(defaultValue = "OPERATION_DEFAULT_VALUE") String optionalValue) {}
}
