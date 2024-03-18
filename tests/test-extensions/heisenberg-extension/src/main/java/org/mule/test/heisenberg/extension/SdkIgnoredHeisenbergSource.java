/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.sdk.api.annotation.Ignore;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;

@Alias("sdk-ignored")
@EmitsResponse
@MediaType(TEXT_PLAIN)
@Ignore
public class SdkIgnoredHeisenbergSource extends Source<String, String> {

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}
