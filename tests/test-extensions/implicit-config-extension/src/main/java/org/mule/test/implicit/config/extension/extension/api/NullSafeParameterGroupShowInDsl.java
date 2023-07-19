/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.config.extension.extension.api;

import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class NullSafeParameterGroupShowInDsl {

  @Parameter
  @Optional
  @NullSafe
  private NullSafePojo nullSafePojoShowInDsl;

  public NullSafePojo getNullSafePojoShowInDsl() {
    return nullSafePojoShowInDsl;
  }

  public void setNullSafePojoShowInDsl(NullSafePojo nullSafePojoShowInDsl) {
    this.nullSafePojoShowInDsl = nullSafePojoShowInDsl;
  }
}
