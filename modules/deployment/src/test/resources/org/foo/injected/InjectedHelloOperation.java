/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.injected;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class InjectedHelloOperation {

  public InjectedHelloOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config InjectedHelloExtension config) {
    System.out.println("Test plugin extension says: " + config.getMessage());
    return config.getMessage();
  }
}
