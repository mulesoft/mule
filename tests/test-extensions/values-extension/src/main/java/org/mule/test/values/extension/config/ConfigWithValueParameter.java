/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.connection.ValuesConnectionProvider;
import org.mule.test.values.extension.resolver.SimpleLegacyValueProvider;

@Configuration(name = "config-with-value")
@ConnectionProviders(ValuesConnectionProvider.class)
public class ConfigWithValueParameter {

  @Parameter
  @OfValues(SimpleLegacyValueProvider.class)
  String channel;
}
