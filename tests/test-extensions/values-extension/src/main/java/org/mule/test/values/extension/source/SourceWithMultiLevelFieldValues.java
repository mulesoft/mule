/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.FieldValues;
import org.mule.test.values.extension.resolver.SdkMultiLevelValueProvider;
import org.mule.test.values.extension.resolver.SimpleValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceWithMultiLevelFieldValues extends AbstractSdkSource {

  @FieldValues(targetSelectors = "channel", value = SimpleValueProvider.class)
  @FieldValues(targetSelectors = {"location.continent", "location.country", "location.city"},
      value = SdkMultiLevelValueProvider.class)
  @Parameter
  String xmlBodyTemplate;
}
