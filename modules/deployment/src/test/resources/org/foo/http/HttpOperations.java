/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.http;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains the operations for the test HTTP plugin using org.foo.http package.
 */
public class HttpOperations {

  public HttpOperations() {}

  @MediaType(value = TEXT_PLAIN, strict = false)
  public String printMessage(@Config HttpExtension config) {
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
  public void nonBlockingPrintMessage(@Config HttpExtension config, CompletionCallback<String, Void> callback) {
    System.out.println("Test plugin extension says: " + config.getMessage());
    Result<String, Void> result = Result.<String, Void>builder()
      .output(config.getMessage())
      .build();
    callback.success(result);
  }
}