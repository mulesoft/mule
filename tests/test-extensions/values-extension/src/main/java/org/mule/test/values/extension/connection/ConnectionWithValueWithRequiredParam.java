/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.WithRequiredParametersFromConfigLegacyValueProvider;

@Alias("WithValueWithRequiredParam")
public class ConnectionWithValueWithRequiredParam extends AbstractConnectionProvider {

  @Parameter
  @OfValues(WithRequiredParametersFromConfigLegacyValueProvider.class)
  String channel;

  @Parameter
  String required1;

  @Parameter
  String required2;
}
