/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
