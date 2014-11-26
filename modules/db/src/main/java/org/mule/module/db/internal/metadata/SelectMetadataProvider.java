/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.metadata;

import org.mule.common.DefaultResult;
import org.mule.common.FailureType;
import org.mule.common.Result;
import org.mule.common.metadata.DefaultDefinedMapMetaDataModel;
import org.mule.common.metadata.DefaultListMetaDataModel;
import org.mule.common.metadata.DefaultMetaData;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataModel;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.result.resultset.ResultSetIterator;

import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides metadata for select queries
 */
public class SelectMetadataProvider extends AbstractQueryMetadataProvider
{

    public static final String DUPLICATE_COLUMN_LABEL_ERROR = "Query metadata contains multiple columns with the same label. Define column aliases to resolve this problem";

    private final boolean streaming;

    public SelectMetadataProvider(DbConfigResolver dbConfigResolver, Query query, boolean streaming)
    {
        super(dbConfigResolver, query);
        this.streaming = streaming;
    }

    @Override
    public Result<MetaData> getStaticOutputMetadata()
    {
        return null;
    }

    @Override
    public Result<MetaData> getDynamicOutputMetadata(PreparedStatement statement)
    {
        ResultSetMetaData statementMetaData;
        try
        {
            statementMetaData = statement.getMetaData();
        }
        catch (SQLException e)
        {
            return new DefaultResult<MetaData>(null, Result.Status.FAILURE, e.getMessage(), FailureType.UNSPECIFIED, e);
        }

        if (statementMetaData == null)
        {
            return new DefaultResult<MetaData>(null, Result.Status.FAILURE, "Driver did not return metadata for the provided SQL");
        }

        Map<String, MetaDataModel> recordModels = new HashMap<String, MetaDataModel>();
        try
        {
            for (int i = 1; i <= statementMetaData.getColumnCount(); i++)
            {
                int columnType = statementMetaData.getColumnType(i);
                recordModels.put(statementMetaData.getColumnLabel(i), getDataTypeMetadataModel(columnType));
            }

            if (statementMetaData.getColumnCount() != recordModels.size())
            {
                return new DefaultResult<MetaData>(null, Result.Status.FAILURE, DUPLICATE_COLUMN_LABEL_ERROR);
            }
        }
        catch (SQLException e)
        {
            return new DefaultResult<MetaData>(null, Result.Status.FAILURE, e.getMessage(), FailureType.UNSPECIFIED, e);
        }


        DefaultDefinedMapMetaDataModel recordModel = new DefaultDefinedMapMetaDataModel(recordModels);
        DefaultListMetaDataModel listModel = new DefaultListMetaDataModel(recordModel);
        if (streaming)
        {
            listModel.setImplementationClass(ResultSetIterator.class.getName());
        }
        DefaultMetaData defaultMetaData = new DefaultMetaData(listModel);

        return new DefaultResult<MetaData>(defaultMetaData);
    }
}
