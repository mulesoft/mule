/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.metadata;

import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.core.internal.metadata.DefaultDataTypeBuilder;

public final class DefaultDataTypeBuilderFactory extends AbstractDataTypeBuilderFactory {

  @Override
  protected DataTypeBuilder create() {
    return new DefaultDataTypeBuilder();
  }

  @Override
  protected DataTypeBuilder create(DataType dataType) {
    return new DefaultDataTypeBuilder(dataType);
  }
}
