/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata.context;

import org.mule.runtime.api.util.LazyValue;

import java.util.function.Function;

public class DefaultParameterInfo<P> implements ValueProviderCacheIdGeneratorContext.ParameterInfo<P> {

  private final String parameterName;
  private final P parameterValue;
  private final LazyValue<Integer> hash;

  public DefaultParameterInfo(String parameterName, P parameterValue, Function<P, Integer> hashingFunction) {
    this.parameterName = parameterName;
    this.parameterValue = parameterValue;
    this.hash = new LazyValue<>(() -> hashingFunction.apply(parameterValue));
  }

  @Override
  public String getName() {
    return parameterName;
  }

  @Override
  public int getHashValue() {
    return hash.get();
  }

  @Override
  public P getValue() {
    return parameterValue;
  }
}
