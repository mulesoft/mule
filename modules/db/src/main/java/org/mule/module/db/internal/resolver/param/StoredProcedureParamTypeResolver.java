/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.ResolvedDbType;
import org.mule.module.db.internal.domain.type.UnknownDbTypeException;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Resolves parameter types for stored procedure queries
 */
public class StoredProcedureParamTypeResolver implements ParamTypeResolver
{

    public static final int PARAM_NAME_COLUN_INDEX = 4;
    public static final int TYPE_ID_COLUMN_INDEX = 6;
    public static final int TYPE_NAME_COLUMN_INDEX = 7;

    private static final Log logger = LogFactory.getLog(StoredProcedureParamTypeResolver.class);

    private final Pattern storedProcedureMatcher = Pattern.compile("(?msi)(\\{\\s+)?call\\s* \\s*(\\w+)\\(.*");
    private final DbTypeManager dbTypeManager;

    public StoredProcedureParamTypeResolver(DbTypeManager dbTypeManager)
    {
        this.dbTypeManager = dbTypeManager;
    }

    @Override
    public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException
    {
        DatabaseMetaData dbMetaData = connection.getMetaData();

        String storedProcedureName = getStoredProcedureName(dbMetaData, queryTemplate.getSqlText());
        ResultSet procedureColumns = dbMetaData.getProcedureColumns(connection.getCatalog(), null, storedProcedureName, "%");

        try
        {
            return getStoredProcedureParamTypes(connection, storedProcedureName, procedureColumns);
        }
        finally
        {
            if (procedureColumns != null)
            {
                procedureColumns.close();
            }
        }
    }

    private Map<Integer, DbType> getStoredProcedureParamTypes(DbConnection connection, String storedProcedureName, ResultSet procedureColumns) throws SQLException
    {
        Map<Integer, DbType> paramTypes = new HashMap<Integer, DbType>();

        int position = 1;

        while (procedureColumns.next())
        {
            int typeId = procedureColumns.getInt(TYPE_ID_COLUMN_INDEX);
            String typeName = procedureColumns.getString(TYPE_NAME_COLUMN_INDEX);

            if (logger.isDebugEnabled())
            {
                String name = procedureColumns.getString(PARAM_NAME_COLUN_INDEX);
                logger.debug(String.format("Resolved parameter type: Store procedure: %s Name: %s Index: %s Type ID: %s Type Name: %s", storedProcedureName, name, position, typeId, typeName));
            }

            DbType dbType;
            try
            {
                dbType = dbTypeManager.lookup(connection, typeId, typeName);
            }
            catch (UnknownDbTypeException e)
            {
                // Type was not found in the type manager, but the DB knows about it
                dbType = new ResolvedDbType(typeId, typeName);
            }
            paramTypes.put(position, dbType);
            position++;
        }

        return paramTypes;
    }

    private String getStoredProcedureName(DatabaseMetaData dbMetaData, String sqlText) throws SQLException
    {
        Matcher matcher = storedProcedureMatcher.matcher(sqlText);

        if (!matcher.matches())
        {
            throw new SQLException("Unable to detect stored procedure name from " + sqlText);
        }

        String storedProcedureName = matcher.group(2);

        if (dbMetaData.storesUpperCaseIdentifiers())
        {
            return storedProcedureName.toUpperCase();
        }
        else
        {
            return storedProcedureName;
        }
    }
}
