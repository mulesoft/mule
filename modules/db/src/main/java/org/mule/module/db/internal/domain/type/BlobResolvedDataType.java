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
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Defines a data type for {@link Blob}
 */
public class BlobResolvedDataType extends ResolvedDbType
{
    public BlobResolvedDataType(int id, String name)
    {
        super(id, name);
    }

    @Override
    public void setParameterValue(PreparedStatement statement, int index, Object value) throws SQLException
    {
        if (value != null && !(value instanceof Blob))
        {
            Blob blob = statement.getConnection().createBlob();
            if (value instanceof byte[])
            {
                blob.setBytes(1, (byte[]) value);
            }
            else if (value instanceof InputStream)
            {
                blob.setBytes(1,  IOUtils.toByteArray((InputStream) value));
            }
            else
            {
                throw new IllegalArgumentException(createUnsupportedTypeErrorMessage(value));
            }
            value = blob;
        }

        super.setParameterValue(statement, index, value);
    }

    protected static String createUnsupportedTypeErrorMessage(Object value) {
        return format("Cannot create a Blob from a value of type '%s'", value.getClass());
    }
}
