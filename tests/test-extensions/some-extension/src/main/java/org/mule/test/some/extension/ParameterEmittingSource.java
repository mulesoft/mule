/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.time.ZonedDateTime;

@MediaType(ANY)
public class ParameterEmittingSource extends Source<Object, Void> {

  @Parameter
  private ZonedDateTime zonedDateTime;

  @Override
  public void onStart(SourceCallback<Object, Void> sourceCallback) {
    sourceCallback.handle(Result.<Object, Void>builder().output(zonedDateTime).build());
  }

  @Override
  public void onStop() {}
}
