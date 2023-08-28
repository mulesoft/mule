/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class HasDoor {

  @Parameter
  private Door commonName;

  public Door getCommonName() {
    return this.commonName;
  }
}
