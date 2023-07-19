/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.builder;

import static org.mockito.Mockito.doReturn;

import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.Transformer;

public abstract class AbstractMockConverterBuilder<T extends AbstractMockConverterBuilder<T>>
    extends AbstractMockTransformerBuilder<T> {

  private int weight;

  public T weighting(int weight) {
    this.weight = weight;
    return getThis();
  }

  @Override
  public Converter build() {
    Transformer converter = super.build();
    doReturn(weight).when((Converter) converter).getPriorityWeighting();

    return (Converter) converter;
  }

  @Override
  protected Class<? extends Transformer> getClassToMock() {
    return Converter.class;
  }
}
