/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.type.DbType;

import java.sql.ParameterMetaData;
import java.sql.SQLException;

/**
 * Builds {@link ParameterMetaData} mocks
 */
public class ParameterMetaDataBuilder
{

    private final ParameterMetaData parameterMetaData = mock(ParameterMetaData.class);

    public ParameterMetaDataBuilder withParameter(int index, DbType type)
    {

        try
        {
            when(parameterMetaData.getParameterType(index)).thenReturn(type.getId());
            when(parameterMetaData.getParameterTypeName(index)).thenReturn(type.getName());
        }
        catch (SQLException e)
        {
            // Not going to happen when building the mock
        }

        return this;
    }

    public ParameterMetaData build()
    {
        return parameterMetaData;
    }
}
