/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.metadata;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.api.metadata.DataTypeBuilderFactory;

public class DefaultDataTypeBuilderFactory extends DataTypeBuilderFactory
{

    @Override
    protected <T> DataTypeBuilder<T> builder()
    {
        return new DefaultDataTypeBuilder<>();
    }

    @Override
    protected <T> DataTypeBuilder<T> builder(DataType<T> dataType)
    {
        return new DefaultDataTypeBuilder<>(dataType);
    }
}
