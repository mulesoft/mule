/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.hello;

import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.service.test.FooService;
import org.mule.runtime.core.internal.client.DefaultLocalMuleClient;

import javax.inject.Inject;

@Extension(
    name = "Privileged",
    description = "Extension for testing purposes")
@Operations({PrivilegedOperation.class})
public class PrivilegedExtension {

  @Parameter
  private String message;

  public PrivilegedExtension() {
    // Moved to the constructor as same code in the operation does not show any error in the log
    InternalMessage.builder().payload("privileged");
  }

  public String getMessage() {
    return message;
  }
}
