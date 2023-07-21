/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.source;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.values.OfValues;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.test.values.extension.resolver.TrueFalseValueProvider;

@MediaType(TEXT_PLAIN)
public class SourceMustNotStart extends AbstractSdkSource {

  public static boolean isStarted = false;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {
    isStarted = true;
    super.onStart(sourceCallback);
  }

  @OfValues(TrueFalseValueProvider.class)
  @Parameter
  String hasBeenStarted;

}
