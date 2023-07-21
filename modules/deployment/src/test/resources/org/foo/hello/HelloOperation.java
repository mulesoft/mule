/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.hello;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

public class HelloOperation {

  public HelloOperation() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config HelloExtension config) {
    System.out.println("Test plugin extension says: " + config.getMessage());
    return config.getMessage();
  }

  /**
   * This is just the same as {@link #printMessage(HelloExtension)} with the only difference that it is declared as non-blocking.
   * <p>
   * Note that the implementation may not be trully non-blocking, it is just used for testing purposes, simulating a non-blocking
   * operation.
   *
   * @param config the config for the operation.
   * @param callback the completion callback.
   */
  @MediaType(value = TEXT_PLAIN, strict = false)
  public void nonBlockingPrintMessage(@Config HelloExtension config, CompletionCallback<String, Void> callback) {
    System.out.println("Test plugin extension says: " + config.getMessage());
    Result<String, Void> result = Result.<String, Void>builder()
      .output(config.getMessage())
      .build();
    callback.success(result);
  }
}
