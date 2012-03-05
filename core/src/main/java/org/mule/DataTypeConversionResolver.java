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
