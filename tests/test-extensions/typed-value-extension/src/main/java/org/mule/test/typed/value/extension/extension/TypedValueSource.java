/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.typed.value.extension.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@Alias("source")
@MediaType(TEXT_PLAIN)
public class TypedValueSource extends Source<String, Object> {

  public static TypedValue<String> onSuccessValue;

  @Override
  public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    sourceCallback.handle(Result.<String, Object>builder().output("This is a string").build());
  }

  @Override
  public void onStop() {

  }

  @OnSuccess
  public void onSuccess(TypedValue<String> stringValue) {
    onSuccessValue = stringValue;
  }

  @OnTerminate
  public void onTerminate() {

  }
}
