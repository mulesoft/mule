/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.config.extension.extension;

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
