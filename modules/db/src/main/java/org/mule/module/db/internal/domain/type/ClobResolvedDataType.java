/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import static java.lang.String.format;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a data type for {@link Clob}
 */
public class ClobResolvedDataType extends ResolvedDbType
{
    public ClobResolvedDataType(int id, String name)
    {
        super(id, name);
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        if (value != null && !(value instanceof Clob))
        {
            if (value instanceof String)
            {
                statement.setCharacterStream(index, new StringReader((String) value), ((String) value).length());
            }
            else if (value instanceof InputStream)
            {
                String stringValue = IOUtils.toString((InputStream) value);
                statement.setCharacterStream(index, new StringReader(stringValue), stringValue.length());
            }
            else
            {
                throw new IllegalArgumentException(createUnsupportedTypeErrorMessage(value));
            }

            return;
        }

        super.setParameterValue(statement, index, value);
    }

    protected static String createUnsupportedTypeErrorMessage(Object value) {
        return format("Cannot create a Clob from a value of type '%s'", value.getClass());
    }
}
