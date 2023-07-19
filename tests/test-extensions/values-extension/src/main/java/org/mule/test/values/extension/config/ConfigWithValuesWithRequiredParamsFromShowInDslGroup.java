/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.GroupWithValuesParameter;
import org.mule.test.values.extension.connection.ValuesConnectionProvider;
import org.mule.test.values.extension.resolver.WithRequiredParameterFromGroupValueProvider;

@Configuration(name = "ValuesWithRequiredParamsFromShowInDslGroup")
@ConnectionProviders(ValuesConnectionProvider.class)
public class ConfigWithValuesWithRequiredParamsFromShowInDslGroup {

  @Parameter
  @OfValues(WithRequiredParameterFromGroupValueProvider.class)
  String valueParam;

  @org.mule.sdk.api.annotation.param.ParameterGroup(name = "someGroup", showInDsl = true)
  GroupWithValuesParameter paramGroup;
}
