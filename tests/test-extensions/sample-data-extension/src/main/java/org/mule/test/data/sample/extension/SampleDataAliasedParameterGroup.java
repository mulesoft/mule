/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.data.sample.extension;

import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;

public class SampleDataAliasedParameterGroup {

  @Parameter
  @Alias("payload")
  private String aliasedPayload;

  @Parameter
  @Optional
  @Alias("attributes")
  private String aliasedAttributes;

  public String getAliasedPayload() {
    return aliasedPayload;
  }

  public String getAliasedAttributes() {
    return aliasedAttributes;
  }
}
