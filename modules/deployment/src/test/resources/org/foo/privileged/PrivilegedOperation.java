/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.hello;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class PrivilegedOperation {

  public PrivilegedOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config PrivilegedExtension config) {
    System.out.println("Test privileged extension says: " + config.getMessage());
    return config.getMessage();
  }
}
