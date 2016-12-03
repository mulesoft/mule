/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.el;

import org.mule.api.MuleContext;
import org.mule.module.db.internal.domain.connection.DbConnection;

import java.sql.SQLException;

/**
 * Provides a MEL function that creates {@link java.sql.Struct} instances.
 * <p/>
 * Function receives three arguments:
 * _ Name of the DB config that will be used to create the value
 * _ Name of the type corresponding to the struct to be created
 * _ A array of list containing the values required to populate the struct. Values must be ordered
 *   following the order in which they are declared in type definition.
 *
 * In order to work, this function requires an active transaction on the provided DB config. This
 * requirement is forced so created values are used inside the same connection and to avoid leaked
 * connections.
 */
public class DbCreateStructFunction extends AbstractDbFunction
{

    public static final String DB_CREATE_STRUCT_FUNCTION = "dbCreateStruct";

    DbCreateStructFunction(MuleContext muleContext)
    {
        super(muleContext);
    }

    @Override
    protected Object createValue(DbConnection connection, String typeName, Object[] values) throws SQLException
    {
        return connection.createStruct(typeName, values);
    }

    @Override
    protected String getFunctionName()
    {
        return DB_CREATE_STRUCT_FUNCTION;
    }
}
