/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.secure;

import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Text;

public class SecureOperations {

  public String dummyOperation(@Password String secureParam, @Text String longText) {
    return secureParam;
  }
}
