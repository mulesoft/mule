/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@MediaType(ANY)
public class AnotherExclusiveOptionalsEmittingSource extends Source<Object, Void> {

  @ParameterGroup(name = "Alias Parameter Group", showInDsl = true)
  SomeAliasedParameterGroupOneRequiredConfig someAliasedParameterGroup;

  @ParameterGroup(name = "Parameter Group")
  SomeParameterGroupOneRequiredConfig someParameterGroup;

  @Parameter
  @Optional
  @Alias(value = "some-parameter-alias")
  private String aliasedSomeParameter;

  @Override
  public void onStart(SourceCallback<Object, Void> sourceCallback) {
    Pair<SomeAliasedParameterGroupOneRequiredConfig, ComplexParameter> parameters = new Pair<>(someAliasedParameterGroup,
                                                                                               someParameterGroup != null
                                                                                                   ? someParameterGroup
                                                                                                       .getComplexParameter()
                                                                                                   : null);
    sourceCallback.handle(Result.<Object, Void>builder().output(parameters).build());
  }

  @Override
  public void onStop() {}
}
