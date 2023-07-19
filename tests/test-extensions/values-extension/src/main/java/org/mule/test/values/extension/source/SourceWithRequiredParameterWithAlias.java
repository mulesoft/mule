/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.test.values.extension.WithRequiredParameterWithAliasGroup;

@MediaType(TEXT_PLAIN)
public class SourceWithRequiredParameterWithAlias extends AbstractSdkSource {

  @org.mule.sdk.api.annotation.param.ParameterGroup(name = "someGroup")
  WithRequiredParameterWithAliasGroup group;
}
