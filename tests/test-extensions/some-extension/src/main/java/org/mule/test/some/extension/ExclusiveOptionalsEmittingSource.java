/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.sdk.api.annotation.param.Optional;
import org.mule.sdk.api.annotation.param.Parameter;

@MediaType(ANY)
public class ExclusiveOptionalsEmittingSource extends Source<Object, Void> {

  @ParameterGroup(name = "Parameter Group", showInDsl = true)
  SomeParameterGroupOneRequiredConfig someParameterGroup;

  @Parameter
  @Optional
  ComplexParameter pojoParameter;

  @Parameter
  @Optional
  String repeatedNameParameter;

  @Override
  public void onStart(SourceCallback<Object, Void> sourceCallback) {
    Pair<SomeParameterGroupOneRequiredConfig, ComplexParameter> parameters = new Pair<>(someParameterGroup, pojoParameter);
    sourceCallback.handle(Result.<Object, Void>builder().output(parameters).build());
  }

  @Override
  public void onStop() {}
}
