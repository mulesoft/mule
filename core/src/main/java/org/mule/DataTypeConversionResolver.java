/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;

import java.util.List;

/**
 * Resolves a conversion from one data type to another returning a converter
 *  {@link Transformer} that is able to convert the data.
 */
public interface DataTypeConversionResolver
{
    Transformer resolve(DataType<?> sourceType, List<DataType<?>> targetDataTypes);
}
