/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

import javax.inject.Inject;

@Alias("Source")
public class JavaxInjectCompatibilityTestSource extends Source<String, String> {

  @Config
  private JavaxInjectCompatibilityTestConfiguration config;

  @Inject
  private ArtifactEncoding encoding;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {
    sourceCallback.handle(Result.<String, String>builder()
        .output(encoding.getDefaultEncoding().name())
        .attributes(config.getEncoding().getDefaultEncoding().name())
        .build());

  }

  @Override
  public void onStop() {
    // nothing to do
  }

}
