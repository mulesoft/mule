/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
