/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a type that is dynamically resolved
 */
public class DynamicDbType implements DbType
{

    private String name;

    public DynamicDbType(String name)
    {
        this.name = name;
    }

    @Override
    public int getId()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Object getParameterValue(CallableStatement statement, int index) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerOutParameter(CallableStatement statement, int index) throws SQLException
    {
        throw new UnsupportedOperationException();
    }
}
