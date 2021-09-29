/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.Optional;

public class StereotypeResolution {

  private final Optional<StereotypeModel> stereotype;
  private final boolean validator;

  public StereotypeResolution(Optional<StereotypeModel> stereotype, boolean validator) {
    this.stereotype = stereotype;
    this.validator = validator;
  }

  public Optional<StereotypeModel> getStereotype() {
    return stereotype;
  }

  public boolean isValidator() {
    return validator;
  }
}
