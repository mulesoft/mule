/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.lang.String.format;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.List;

/**
 * Defines a structured data type
 */
public class StructDbType extends AbstractStructuredDbType
{

    /**
     * Creates a new DB type
     *
     * @param id type identifier from {#link java.sql.Types} or any custom value.
     * @param name name of the structured type. Non empty.
     */
    public StructDbType(int id, String name)
    {
        super(id, name);
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        if (value != null && !(value instanceof Struct))
        {
            Connection connection = statement.getConnection();
            if (value instanceof Object[])
            {
                value = connection.createStruct(name, (Object[]) value);
            }
            else if (value instanceof List)
            {
                value = connection.createStruct(name, ((List) value).toArray());
            }
            else
            {
                throw new IllegalArgumentException(createUnsupportedTypeErrorMessage(value));
            }
        }

        super.setParameterValue(statement, index, value);
    }

    /**
     * Creates error message for the case when a given class is not supported
     *
     * @param value value that was attempted to be converted
     * @return the error message for the provided value's class
     */
    protected static String createUnsupportedTypeErrorMessage(Object value) {
        return format("Cannot create a %s from a value of type '%s'", Struct.class.getName(), value.getClass());
    }
}
