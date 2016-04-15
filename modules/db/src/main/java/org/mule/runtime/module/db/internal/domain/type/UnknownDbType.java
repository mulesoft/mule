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
import java.sql.Types;

/**
 * Represents a data type for a template, which real type is unknown until it is instantiated
 */
public class UnknownDbType extends AbstractDbType
{

    public static final String UNKNOWN_TYPE_NAME = "UNKNOWN";

    private static final UnknownDbType instance = new UnknownDbType();

    private UnknownDbType()
    {
        super(Types.OTHER, UNKNOWN_TYPE_NAME);
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        statement.setObject(index, value);
    }

    @Override
    public Object getParameterValue(CallableStatement statement, int index) throws SQLException
    {
        return statement.getObject(index);
    }

    public static DbType getInstance()
    {
        return instance;
    }
}
