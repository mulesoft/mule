/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.builder;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.util.Arrays;

import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public abstract class AbstractMockTransformerBuilder<T extends AbstractMockTransformerBuilder> {

  protected String name;
  protected DataType resultDataType;
  protected DataType[] sourceDataTypes;
  protected Object value;

  public T named(String name) {
    this.name = name;
    return getThis();
  }

  public T from(DataType... sourceDataTypes) {
    this.sourceDataTypes = sourceDataTypes;
    return getThis();
  }

  public T to(DataType resultDataType) {
    this.resultDataType = resultDataType;
    return getThis();
  }

  public T returning(Object value) {
    this.value = value;
    return getThis();
  }

  public Transformer build() {
    Transformer transformer;
    if (name == null || name.isEmpty()) {

      transformer = mock(getClassToMock());
    } else {
      transformer = mock(getClassToMock(), name);
      doReturn(name).when(transformer).getName();
    }

    if (resultDataType != null) {
      doReturn(resultDataType).when(transformer).getReturnDataType();
    }
    if (sourceDataTypes != null) {
      doReturn(Arrays.asList(sourceDataTypes)).when(transformer).getSourceDataTypes();

      when(transformer.isSourceDataTypeSupported((DataType) argThat(new SupportsSourceDataType()))).thenReturn(true);
    }
    try {
      doReturn(value).when(transformer).transform(Mockito.any(Object.class));
    } catch (TransformerException e) {
      // Not going to happen during mock setup
    }

    return transformer;
  }

  protected Class<? extends Transformer> getClassToMock() {
    return Transformer.class;
  }

  @SuppressWarnings({"unchecked"})
  protected T getThis() {
    return (T) this;
  }

  class SupportsSourceDataType extends ArgumentMatcher {

    @Override
    public boolean matches(Object value) {
      DataType dataType = (DataType) value;

      for (DataType sourceDataType : sourceDataTypes) {
        if (sourceDataType.isCompatibleWith(dataType)) {
          return true;
        }
      }

      return false;
    }
  }
}
