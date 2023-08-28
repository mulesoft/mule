/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.Connection;

public class GrapeOperations {

  public VeganPolicy grapeOperation(@Connection VeganPolicy policy) {
    return policy;
  }
}
