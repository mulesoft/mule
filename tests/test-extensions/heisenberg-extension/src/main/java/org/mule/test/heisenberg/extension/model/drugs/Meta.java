/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.model.drugs;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class Meta implements Drug {

  @Parameter
  public int purity;

  public int getPurity() {
    return purity;
  }
}
