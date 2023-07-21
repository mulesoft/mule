/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.FieldValues;
import org.mule.test.values.extension.resolver.SimpleValueProvider;

@MediaType(TEXT_PLAIN)
public class SimpleSourceWithParameterWithFieldValues extends AbstractSdkSource {

  @FieldValues(targetSelectors = "simple.path", value = SimpleValueProvider.class)
  @Parameter
  String channel;

}
