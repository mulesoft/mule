/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.values.extension.resolver.WithRequiredParameterWithAliasValueProvider;

public class WithRequiredParameterWithAliasGroup {

  @Parameter
  @Alias("superString")
  String requiredString;

  @Parameter
  @OfValues(WithRequiredParameterWithAliasValueProvider.class)
  String channels;
}
