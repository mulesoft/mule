/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.GroupWithValuesParameter;
import org.mule.test.values.extension.resolver.WithRequiredParameterFromGroupLegacyValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceWithRequiredParameterInsideShowInDslGroup extends AbstractSdkSource {

  @OfValues(WithRequiredParameterFromGroupLegacyValueProvider.class)
  @Parameter
  String values;

  @ParameterGroup(name = "ValuesGroup", showInDsl = true)
  GroupWithValuesParameter optionsParameter;
}
