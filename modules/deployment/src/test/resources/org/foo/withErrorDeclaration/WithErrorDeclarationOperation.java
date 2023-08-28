/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.withErrorDeclaration;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class WithErrorDeclarationOperation {

  public WithErrorDeclarationOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config WithErrorDeclarationExtension config) {
    System.out.println("WithErrorDeclaration extension says: " + config.getMessage());
    return config.getMessage();
  }
}
