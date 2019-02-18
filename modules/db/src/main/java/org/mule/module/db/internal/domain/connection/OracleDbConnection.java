/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.module.db.internal.domain.connection.type.resolver.CollectionTypeResolver;
import org.mule.module.db.internal.domain.connection.type.resolver.TypeResolver;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.domain.type.ResolvedDbType;
import org.mule.module.db.internal.resolver.param.ParamTypeResolverFactory;

import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.Entry;
import static org.mule.module.db.internal.domain.connection.oracle.OracleConnectionUtils.getOwnerFrom;
import static org.mule.module.db.internal.domain.connection.oracle.OracleConnectionUtils.getTypeSimpleName;

/**
 * Custom {@link DbConnection} for Oracle databases
 */
public class OracleDbConnection extends DefaultDbConnection
{
    public static final String ATTR_TYPE_NAME_PARAM = "ATTR_TYPE_NAME";

    public static final String ATTR_NO_PARAM = "ATTR_NO";

    public static final String QUERY_TYPE_ATTRS = "SELECT ATTR_NO, ATTR_TYPE_NAME FROM ALL_TYPE_ATTRS WHERE TYPE_NAME = ? AND ATTR_TYPE_NAME IN ('CLOB', 'BLOB')";

    public static final String QUERY_OWNER_CONDITION = " AND OWNER = ?";

    private Method createArrayMethod;
    private boolean initialized;

    /**
     * {@inheritDoc}
     */
    public OracleDbConnection(Connection delegate, TransactionalAction transactionalAction, DefaultDbConnectionReleaser connectionReleaseListener, ParamTypeResolverFactory paramTypeResolverFactory)
    {
        super(delegate, transactionalAction, connectionReleaseListener, paramTypeResolverFactory);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException
    {
        if (getCreateArrayOfMethod(delegate) == null)
        {
            return super.createArrayOf(typeName, elements);
        }
        else
        {
            try
            {
                resolveLobs(typeName, elements, new CollectionTypeResolver(this));
                return (Array) getCreateArrayOfMethod(delegate).invoke(delegate, typeName, elements);
            }
            catch (Exception e)
            {
                throw new SQLException("Error creating ARRAY", e);
            }
        }
    }

    private Method getCreateArrayOfMethod(Connection delegate)
    {
        if (createArrayMethod == null && !initialized)
        {
            synchronized (this)
            {
                if (createArrayMethod == null && !initialized)
                {
                    try
                    {
                        createArrayMethod = delegate.getClass().getMethod("createARRAY", String.class, Object.class);
                        createArrayMethod.setAccessible(true);
                    }
                    catch (NoSuchMethodException e)
                    {
                        // Ignore, will use the standard method
                    }

                    initialized = true;
                }
            }
        }

        return createArrayMethod;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return super.createStruct(typeName, attributes);
    }

    @Override
    protected void resolveLobs(String typeName, Object[] attributes, TypeResolver typeResolver) throws SQLException
    {
        Map<Integer, ResolvedDbType> dataTypes = getLobFieldsDataTypeInfo(typeResolver.resolveType(typeName));

        if (dataTypes.keySet().isEmpty())
        {
            logger.warn("No catalog information was found for the typename {}. No lob resolution will be performed", typeName);
        }

        for (Entry entry : dataTypes.entrySet())
        {
            Integer index = (Integer) entry.getKey();
            ResolvedDbType dataType = (ResolvedDbType) entry.getValue();
            // In Oracle we do not have the data type for structs or arrays, as the
            // the driver does not provide the getAttributes functionality  
            // in their DatabaseMetaData.
            // It has to be taken into account that the data type depends on JDBC, so the
            // driver is the unit responsible for the mapping and we do not have that information
            // in the DB catalog. We resolve the lobs depending on the name only.
            typeResolver.resolveLobs(attributes, index-1, dataType.getName());
        }
    }

    @Override
    protected Map<Integer, ResolvedDbType> getLobFieldsDataTypeInfo(String typeName) throws SQLException
    {
        Map<Integer, ResolvedDbType> dataTypes = new HashMap<>();

        String owner = getOwnerFrom(typeName);
        String type = getTypeSimpleName(typeName);

        String query = QUERY_TYPE_ATTRS + (owner != null ? QUERY_OWNER_CONDITION : "");

        try (PreparedStatement ps = this.prepareStatement(query))
        {
            ps.setString(1, type);
            if (owner != null)
            {
                ps.setString(2, owner);
            }

            try (ResultSet resultSet = ps.executeQuery())
            {
                while (resultSet.next())
                {
                    ResolvedDbType resolvedDbType = new ResolvedDbType(UNKNOWN_DATA_TYPE, resultSet.getString(ATTR_TYPE_NAME_PARAM));
                    dataTypes.put(resultSet.getInt(ATTR_NO_PARAM), resolvedDbType);
                }
            }

            return dataTypes;
        }
    }
}
