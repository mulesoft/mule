/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.privileged.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

public class PrivilegedOperations {

  @MediaType(TEXT_PLAIN)
  public String doSomething() {
    return "I did something";
  }

  @MediaType(TEXT_PLAIN)
  public void doSomethingAsync(CompletionCallback<String, Void> completionCallback) {

  }
}
