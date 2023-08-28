/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension;


import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SampleDataAliasedParameterGroup {

  @Parameter
  @Alias("aliasedPayload")
  private String payload;

  @Parameter
  @Optional
  @Alias("aliasedAttributes")
  private String attributes;

  public String getPayload() {
    return payload;
  }

  public String getAttributes() {
    return attributes;
  }
}
