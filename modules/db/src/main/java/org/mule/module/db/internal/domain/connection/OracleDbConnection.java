/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.mule.module.db.internal.domain.transaction.TransactionalAction;
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

    public static final String QUERY_TYPE_OWNER_CONDITION = " AND OWNER = ?";

    public static final String QUERY_ALL_COLL_TYPES = "SELECT * FROM SYS.ALL_COLL_TYPES WHERE TYPE_NAME = ?";

    private static final String ELEM_TYPE_NAME = "ELEM_TYPE_NAME";

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
                resolveLobsForArrays(typeName, elements);
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
    protected void resolveLobsForArrays(String typeName, Object[] elements) throws SQLException
    {
        String collectionTypeName = getTypeFor(typeName);

        if(collectionTypeName != null)
        {
            typeName = collectionTypeName;
        }

        Map<Integer, String> dataTypes = getLobFieldsDataTypeInfo(typeName);

        if (dataTypes.keySet().isEmpty())
        {
            logger.warn("No catalog information was found for the typename {}. No lob resolution will be performed", typeName);
        }

        Integer index;
        for (Map.Entry entry : dataTypes.entrySet())
        {
            index = (Integer) entry.getKey();
            String dataTypeName = dataTypes.get(index);

            for(Object element : elements)
            {
                doResolveLobIn((Object[]) element, index-1, dataTypeName);
            }
        }
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException
    {
        return super.createStruct(typeName, attributes);
    }

    @Override
    protected void resolveLobs(String typeName, Object[] attributes) throws SQLException
    {
        Map<Integer, String> dataTypes = getLobFieldsDataTypeInfo(typeName);

        if (dataTypes.keySet().isEmpty())
        {
            logger.warn("No catalog information was found for the typename {}. No lob resolution will be performed", typeName);
        }

        for (int index : dataTypes.keySet())
        {
            String dataTypeName = dataTypes.get(index);
            // In Oracle we do not have the data type for structs, as the 
            // the driver does not provide the getAttributes functionality  
            // in their DatabaseMetaData.
            // It has to be taken into account that the data type depends on JDBC, so the
            // driver is the unit responsible for the mapping and we do not have that information
            // in the DB catalog. We resolve the lobs depending on the name only.
            doResolveLobIn(attributes, index-1, dataTypeName);
        }
    }

    private Map<Integer, String> getLobFieldsDataTypeInfo(String typeName) throws SQLException
    {
        Map<Integer, String> dataTypes = new HashMap<>();

        String owner = getOwnerFrom(typeName);
        String type = getTypeSimpleName(typeName);

        String query = QUERY_TYPE_ATTRS + (owner != null ? QUERY_TYPE_OWNER_CONDITION : "");

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
                    dataTypes.put(resultSet.getInt(ATTR_NO_PARAM), resultSet.getString(ATTR_TYPE_NAME_PARAM));
                }
            }

            return dataTypes;
        }
    }

    private String getTypeFor(String collectionTypeName) throws SQLException
    {
        String dataType = null;

        try (PreparedStatement ps = this.prepareStatement(QUERY_ALL_COLL_TYPES))
        {
            ps.setString(1, collectionTypeName);

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
