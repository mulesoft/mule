/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transformer;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.DataType;

import java.util.List;

/**
 * Resolves a conversion from one data type to another returning a converter {@link Transformer} that is able to convert the data.
 */
@NoImplement
public interface DataTypeConversionResolver {

  Transformer resolve(DataType sourceType, List<DataType> targetDataTypes);
}
