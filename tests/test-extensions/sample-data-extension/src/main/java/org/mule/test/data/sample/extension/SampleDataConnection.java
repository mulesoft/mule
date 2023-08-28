/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.data.sample.extension;

import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.test.data.sample.extension.SampleDataExtension.NULL_VALUE;

import org.mule.runtime.extension.api.runtime.operation.Result;


public class SampleDataConnection {

  public Result<String, String> getResult(String payload, String attributes) {
    return Result.<String, String>builder()
        .output(payload)
        .mediaType(APPLICATION_JSON)
        .attributes(attributes != null ? attributes : NULL_VALUE)
        .attributesMediaType(APPLICATION_XML)
        .build();
  }
}
