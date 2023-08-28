/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.withErrorDeclaration;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.inject.Inject;

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
