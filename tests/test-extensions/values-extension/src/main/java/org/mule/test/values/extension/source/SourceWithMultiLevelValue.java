/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.GroupAsMultiLevelValue;
import org.mule.test.values.extension.resolver.MultiLevelValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceWithMultiLevelValue extends AbstractSdkSource {

  @OfValues(MultiLevelValueProvider.class)
  @ParameterGroup(name = "values")
  GroupAsMultiLevelValue optionsParameter;
}
