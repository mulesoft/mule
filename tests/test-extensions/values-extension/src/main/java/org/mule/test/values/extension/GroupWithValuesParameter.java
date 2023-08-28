/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.SimpleValueProvider;

public class GroupWithValuesParameter {

  @OfValues(SimpleValueProvider.class)
  @Parameter
  String channels;

  @Optional
  @Parameter
  String anyParameter;

}
