/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.connection;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.GroupWithValuesParameter;
import org.mule.test.values.extension.resolver.WithRequiredParameterFromGroupValueProvider;

@Alias("WithValuesWithRequiredParamsFromParamGroup")
public class ConnectionWithValuesWithRequiredParamsFromParamGroup extends AbstractConnectionProvider {

  @Parameter
  @OfValues(WithRequiredParameterFromGroupValueProvider.class)
  String valueParam;

  @ParameterGroup(name = "someGroup")
  GroupWithValuesParameter paramGroup;
}
