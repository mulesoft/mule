/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static org.openjdk.jmh.infra.Blackhole.consumeCPU;

import org.mule.sdk.api.annotation.param.Content;
import org.mule.sdk.api.runtime.operation.Result;

import java.io.InputStream;
import java.util.List;

public class DefaultExtensionsClientTestOperations {

  public void basicNoOp() {
    // Prevents this call from being optimized out
    consumeCPU(10);
  }

  public void multipleParametersNoOp(String stringParameter,
                                     int intParameter,
                                     List<String> strings) {
    // Prevents this call from being optimized out
    consumeCPU(10);
  }

  public String simplePayloadOutput() {
    return "Hello world";
  }

  public Result<String, String> simpleResultOutput() {
    return Result.<String, String>builder()
        .output("Hello world")
        .attributes("Attributes")
        .build();
  }

  public Result<InputStream, Void> simpleIdentity(@Content InputStream content) {
    return Result.<InputStream, Void>builder()
        .output(content)
        .build();
  }
}
