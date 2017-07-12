/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.sql.Types.DECIMAL;
import static java.sql.Types.NUMERIC;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a data type that was resolved for a database instance
 */
public class ResolvedDbType extends AbstractDbType
{

    /**
     * Creates a new DB type
     *
     * @param id type identifier from {#link java.sql.Types} or any custom value.
     * @param name name of the structured type. Non empty.
     */
    public ResolvedDbType(int id, String name)
    {
        super(id, name);
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        if (value == null)
        {
            statement.setNull(index, id);
        }
        else
        {
            if (DECIMAL == id || NUMERIC == id)
            {
                if (value instanceof BigDecimal)
                {
                    statement.setObject(index, value, id, ((BigDecimal) value).scale());
                }
                else if (value instanceof Float || value instanceof Double)
                {
                    BigDecimal bigDecimal = new BigDecimal(value.toString());
                    statement.setObject(index, bigDecimal, id, bigDecimal.scale());
                }
                else
                {
                    statement.setObject(index, value, id);
                }
            }
            else
            {
                statement.setObject(index, value, id);
            }
        }
    }

    @Override
    public Object getParameterValue(CallableStatement statement, int index) throws SQLException
    {
        return statement.getObject(index);
    }
}
