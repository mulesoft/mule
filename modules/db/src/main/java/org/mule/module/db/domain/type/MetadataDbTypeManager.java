/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.domain.type;

import org.mule.module.db.domain.connection.DbConnection;
import org.mule.module.db.result.resultset.ResultSetIterator;
import org.mule.module.db.result.resultset.SingleResultResultSetCloser;
import org.mule.module.db.result.row.InsensitiveMapRowHandler;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Manages database types that are discovered using database metadata
 */
public class MetadataDbTypeManager implements DbTypeManager
{

    private final Log logger = LogFactory.getLog(MetadataDbTypeManager.class);

    private final Map<String, DbType> typesById = new HashMap<String, DbType>();
    private final Object lock = new Object();
    private boolean initialised;

    protected void registerType(DbType dbType)
    {
        String typeKey = dbType.getName() + dbType.getId();
        if (typesById.containsKey(typeKey))
        {
            throw new IllegalArgumentException(String.format("There is already a registered type with ID %s and name %s", dbType.getId(), dbType.getName()));
        }

        typesById.put(typeKey, dbType);
    }

    @Override
    public DbType lookup(DbConnection connection, int id, String name) throws UnknownDbTypeException
    {
        if (!initialised)
        {
            synchronized (lock)
            {
                if (!initialised)
                {
                    initialise(connection);
                    initialised = true;
                }

            }
        }

        String typeKey = name+  id;
        if (typesById.containsKey(typeKey))
        {
            return typesById.get(typeKey);
        }
        else if (id == Types.OTHER)
        {
            return UnknownDbType.getInstance();
        }
        else
        {
            throw new UnknownDbTypeException(id, name);
        }
    }

    @Override
    public DbType lookup(DbConnection connection, String name) throws UnknownDbTypeException
    {
        throw new UnknownDbTypeException(name);
    }

    protected void initialise(DbConnection connection)
    {
        try
        {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet typeInfo = metaData.getTypeInfo();
            ResultSetIterator resultSetIterator = new ResultSetIterator(connection, typeInfo, new InsensitiveMapRowHandler(), new SingleResultResultSetCloser());
            while (resultSetIterator.hasNext())
            {
                Map<String, Object> typeRecord = resultSetIterator.next();

                Number data_type = (Number) typeRecord.get("DATA_TYPE");
                String type_name = (String) typeRecord.get("TYPE_NAME");
                registerType(new ResolvedDbType(data_type.intValue(), type_name));

                if (logger.isDebugEnabled())
                {
                    logger.debug("Type: " + typeRecord);
                }
            }
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Cannot process metadata information", e);
        }
    }
}
