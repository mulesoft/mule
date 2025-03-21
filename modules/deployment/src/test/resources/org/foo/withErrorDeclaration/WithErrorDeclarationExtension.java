/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withErrorDeclaration;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import jakarta.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "WithErrorDeclaration")
@ErrorTypes(WithErrorDeclarationErrors.class)
@Operations({WithErrorDeclarationOperation.class})
public class WithErrorDeclarationExtension {

  public WithErrorDeclarationExtension() {}

  public String getMessage() {
    return "Errors are ugly :(s";
  }
}
