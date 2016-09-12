/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.type;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * Defines a structured data type
 */
public class StructuredDbType extends ResolvedDbType
{

    public StructuredDbType(int id, String name)
    {
        super(id, name);
    }

    @Override
    public void registerOutParameter(CallableStatement statement, int index) throws SQLException
    {
        statement.registerOutParameter(index, id, name);
    }
}
