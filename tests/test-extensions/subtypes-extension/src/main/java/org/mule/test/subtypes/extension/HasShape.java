/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class HasShape {

  @Parameter
  private ParentShape commonName;

  public ParentShape getCommonName() {
    return commonName;
  }
}
