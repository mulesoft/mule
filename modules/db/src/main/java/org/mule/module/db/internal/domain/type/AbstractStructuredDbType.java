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
 * Base class for structured DB types.
 */
public abstract class AbstractStructuredDbType extends ResolvedDbType
{

    /**
     * Creates a new DB type
     *
     * @param id type identifier from {#link java.sql.Types} or any custom value.
     * @param name name of the structured type. Non empty.
     */
    public AbstractStructuredDbType(int id, String name)
    {
        super(id, name);
    }

    @Override
    public void registerOutParameter(CallableStatement statement, int index) throws SQLException
    {
        statement.registerOutParameter(index, id, name);
    }
}
