/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.nonimplicit.config.extension.extension.api;

import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class NullSafeParameterGroup {

  @Parameter
  @Optional
  @NullSafe
  private NullSafePojo nullSafePojo;

  public NullSafePojo getNullSafePojo() {
    return nullSafePojo;
  }

  public void setNullSafePojo(NullSafePojo nullSafePojo) {
    this.nullSafePojo = nullSafePojo;
  }
}
