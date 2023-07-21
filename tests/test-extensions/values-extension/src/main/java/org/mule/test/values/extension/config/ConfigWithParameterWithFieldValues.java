/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.FieldValues;
import org.mule.test.values.extension.connection.ValuesConnectionProvider;
import org.mule.test.values.extension.resolver.SimpleValueProvider;

@Configuration(name = "config-with-parameter-with-field-values")
@ConnectionProviders(ValuesConnectionProvider.class)
public class ConfigWithParameterWithFieldValues {

  @Parameter
  @FieldValues(targetSelectors = "security.algorithm", value = SimpleValueProvider.class)
  String securityHeaders;
}
