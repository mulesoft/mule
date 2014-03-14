/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.domain.type;

import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.domain.query.QueryTemplate;

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
 * Resolves types using query metadata
 */
public class MetadataTypeResolver implements DbTypeResolver
{

    private static final Log logger = LogFactory.getLog(MetadataTypeResolver.class);

    private final DbTypeManager dbTypeManager;
    private final Pattern storedProcedureMatcher = Pattern.compile("(?msi)(\\{\\s+)?call\\s* \\s*(\\w+)\\(.*");

    public MetadataTypeResolver(DbTypeManager dbTypeManager)
    {
        this.dbTypeManager = dbTypeManager;
    }

    @Override
    public Map<Integer, DbType> getParameterTypes(DbConnection connection, QueryTemplate queryTemplate) throws SQLException
    {
        Map<Integer, DbType> paramTypes = new HashMap<Integer, DbType>();

        DatabaseMetaData dbMetaData = connection.getMetaData();

        String storedProcedureName = getStoredProcedureName(dbMetaData, queryTemplate.getSqlText());
        ResultSet procedureColumns = dbMetaData.getProcedureColumns(connection.getCatalog(), null, storedProcedureName, "%");

        int position =1;
        while (procedureColumns.next())
        {
            int typeId= procedureColumns.getInt(6);
            String typeName = procedureColumns.getString(7);

            if (logger.isDebugEnabled())
            {

                String name = procedureColumns.getString(4);
                logger.debug(String.format("Resolved parameter type: Store procedure: %s Name: %s Index: %s Type ID: %s Type Name: %s", storedProcedureName, name, position, typeId, typeName));
            }

            DbType dbType;
            try
            {
                dbType = dbTypeManager.get(typeId, typeName);
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
