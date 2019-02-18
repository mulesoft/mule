/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection.type.resolver;

import org.mule.module.db.internal.domain.connection.DefaultDbConnection;
import org.mule.module.db.internal.domain.type.ResolvedDbType;

import java.sql.SQLException;

/**
 * Type resolver for struct entities
 *
 * @since 3.10.0
 */
public class StructTypeResolver implements TypeResolver
{
    private DefaultDbConnection connection;

    public StructTypeResolver(DefaultDbConnection connection)
    {
        this.connection = connection;
    }

    @Override
    public void resolveLobs(Object[] elements, Integer index, String dataTypeName) throws SQLException
    {
        connection.doResolveLobIn(elements, index, dataTypeName);
    }

    @Override
    public String resolveType(String typeName)
    {
        return typeName;
    }

    @Override
    public void resolveLobIn(Object[] attributes, Integer index, ResolvedDbType resolvedDbType) throws SQLException
    {
        connection.doResolveLobIn(attributes, index, resolvedDbType.getId(), resolvedDbType.getName());
    }
}
