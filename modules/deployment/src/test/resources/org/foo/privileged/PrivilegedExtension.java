/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.hello;

import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;

/**
 * Extension for testing purposes
 */
@Extension(name = "Privileged")
@Operations({PrivilegedOperation.class})
@DeclarationEnrichers({})
public class PrivilegedExtension {

  @Parameter
  private String message;

  public PrivilegedExtension() {
    // Moved to the constructor as same code in the operation does not show any error in the log
    InternalMessage.builder().value("privileged");
  }

  public String getMessage() {
    return message;
  }
}
