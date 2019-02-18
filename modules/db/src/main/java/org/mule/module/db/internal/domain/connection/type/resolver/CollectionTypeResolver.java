/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection.type.resolver;

import org.mule.module.db.internal.domain.connection.DefaultDbConnection;
import org.mule.module.db.internal.domain.type.ResolvedDbType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mule.module.db.internal.domain.connection.OracleDbConnection.QUERY_OWNER_CONDITION;
import static org.mule.module.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.module.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;

/**
 * Type resolver for array entities
 *
 * @since 3.10.0
 */
public class CollectionTypeResolver implements TypeResolver
{
    public static final String QUERY_ALL_COLL_TYPES = "SELECT * FROM SYS.ALL_COLL_TYPES WHERE TYPE_NAME = ?";

    private static final String ELEM_TYPE_NAME = "ELEM_TYPE_NAME";

    private DefaultDbConnection connection;

    public CollectionTypeResolver(DefaultDbConnection connection)
    {
        this.connection = connection;
    }

    @Override
    public void resolveLobs(Object[] elements, Integer index, String dataTypeName) throws SQLException
    {
        for(Object element : elements)
        {
            connection.doResolveLobIn((Object[]) element, index, dataTypeName);
        }
    }

    @Override
    public String resolveType(String typeName) throws SQLException
    {
        String collectionTypeName = getTypeFor(typeName);

        if(collectionTypeName != null)
        {
            return collectionTypeName;
        }
        return typeName;
    }

    @Override
    public void resolveLobIn(Object[] attributes, Integer index, ResolvedDbType resolvedDbType) throws SQLException
    {
        for(Object attribute : attributes)
        {
            connection.doResolveLobIn((Object[]) attribute, index, resolvedDbType.getId(), resolvedDbType.getName());
        }
    }

    private String getTypeFor(String collectionTypeName) throws SQLException
    {
        String dataType = null;

        String owner = getOwnerFrom(collectionTypeName);
        String typeName = getTypeSimpleName(collectionTypeName);
        String query = QUERY_ALL_COLL_TYPES;

        if(owner != null)
        {
            query = query + QUERY_OWNER_CONDITION;
        }

        try (PreparedStatement ps = connection.prepareStatement(query))
        {
            ps.setString(1, typeName);

            if(owner != null)
            {
                ps.setString(2, owner);
            }

            try (ResultSet resultSet = ps.executeQuery())
            {
                while (resultSet.next())
                {
                    dataType = resultSet.getString(ELEM_TYPE_NAME);
                }
            }
        }
        return dataType;
    }
}
