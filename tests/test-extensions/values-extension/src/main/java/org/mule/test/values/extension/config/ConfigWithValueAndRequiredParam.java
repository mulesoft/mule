/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.connection.ValuesConnectionProvider;
import org.mule.test.values.extension.resolver.WithRequiredParametersFromConfigValueProvider;

@Configuration(name = "ValueWithRequiredParam")
@ConnectionProviders(ValuesConnectionProvider.class)
public class ConfigWithValueAndRequiredParam {

  @Parameter
  @OfValues(WithRequiredParametersFromConfigValueProvider.class)
  String channel;

  @Parameter
  String required1;

  @Parameter
  String required2;
}
