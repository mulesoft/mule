/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.test.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.UnknownDbTypeException;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link DbTypeManager} mocks
 */
public class DbTypeManagerBuilder
{

    private DbConnection connection;
    private List<DbType> types = new ArrayList<DbType>();
    private List<DbType> unknownTypes = new ArrayList<DbType>();

    public DbTypeManagerBuilder on(DbConnection connection)
    {

        this.connection = connection;
        return this;
    }

    public DbTypeManagerBuilder managing(DbType type)
    {
        types.add(type);

        return this;
    }

    public DbTypeManagerBuilder unknowing(DbType type)
    {
        unknownTypes.add(type);

        return this;
    }

    public DbTypeManager build()
    {
        DbTypeManager dbTypeManager = mock(DbTypeManager.class);

        for (DbType type : types)
        {
            when(dbTypeManager.lookup(connection, type.getId(), type.getName())).thenReturn(type);
        }

        for (DbType type : unknownTypes)
        {
            when(dbTypeManager.lookup(connection, type.getId(), type.getName())).thenThrow(new UnknownDbTypeException(type.getId(), type.getName()));
        }

        return dbTypeManager;
    }

}
