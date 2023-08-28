/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.nonimplicit.config.extension.extension.internal;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class PrivateParameterGroup {

  @Parameter
  @Optional
  private String privateName;

  public String getPrivateName() {
    return privateName;
  }

  public void setPrivateName(String privateName) {
    this.privateName = privateName;
  }

}
