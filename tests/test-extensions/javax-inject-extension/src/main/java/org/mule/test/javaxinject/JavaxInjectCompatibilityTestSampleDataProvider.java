/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.operation.Result;

import javax.inject.Inject;

public class JavaxInjectCompatibilityTestSampleDataProvider implements SampleDataProvider<String, Void> {

  @Inject
  private ArtifactEncoding encoding;

  public String getId() {
    return "id";
  }

  public Result<String, Void> getSample() throws SampleDataException {
    return Result.<String, Void>builder()
        .output(encoding.getDefaultEncoding().name())
        .build();
  }

}
