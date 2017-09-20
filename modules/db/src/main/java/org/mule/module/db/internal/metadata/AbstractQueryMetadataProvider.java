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
import org.mule.common.metadata.DefaultPojoMetaDataModel;
import org.mule.common.metadata.DefaultSimpleMetaDataModel;
import org.mule.common.metadata.DefaultUnknownMetaDataModel;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.MetaDataModel;
import org.mule.common.metadata.datatype.DataType;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link QueryMetadataProvider} implementations
 */
public abstract class AbstractQueryMetadataProvider implements QueryMetadataProvider
{
    private static final Logger logger = LoggerFactory.getLogger(AbstractQueryMetadataProvider.class);

    protected final Query query;
    protected final DbConfigResolver dbConfigResolver;
    private Map<Integer, MetaDataModel> dbToMetaDataType;

    public AbstractQueryMetadataProvider(DbConfigResolver dbConfigResolver, Query query)
    {
        this.dbConfigResolver = dbConfigResolver;
        this.query = query;
    }

    /**
     *
     * @return output metadata for a type of query. Can be null
     */
    protected abstract Result<MetaData> getStaticOutputMetadata();

    /**
     * Calculates the dynamic output metadata from a statement
     *
     * @param statement statement used to calculate the metadata
     *
     * @return output metadata for the statement. Can be null
     */
    protected abstract Result<MetaData> getDynamicOutputMetadata(PreparedStatement statement);

    /**
     * Calculates the input metadata for a statement
     *
     * @param statement statement used to calculate the metadata
     * @param query query that will be executed in the statement
     * @return
     */
    protected Result<MetaData> getDynamicInputMetadata(PreparedStatement statement, Query query)
    {
        List<InputQueryParam> inputParams = query.getQueryTemplate().getInputParams();

        // No metadata when no input parameters
        if (inputParams.size() == 0)
        {
            return null;
        }

        List<String> fieldNames = new ArrayList<String>();
        for (InputQueryParam inputParam : inputParams)
        {
            String field = getReferencedField(inputParam);
            if (field == null)
            {
                return null;
            }
            fieldNames.add(field);
        }

        try
        {
            return getInputMetadataUsingStatementMetadata(statement, fieldNames);
        }
        catch (SQLException e)
        {
            return getStaticInputMetadata(fieldNames);
        }
    }

    private Result<MetaData> getStaticInputMetadata(List<String> fieldNames)
    {
        Map<String, MetaDataModel> recordModels = new HashMap<String, MetaDataModel>();

        for (String fieldName : fieldNames)
        {
            recordModels.put(fieldName, getDataTypeMetadataModel(Types.VARCHAR));
        }

        DefaultDefinedMapMetaDataModel recordModel = new DefaultDefinedMapMetaDataModel(recordModels);
        DefaultMetaData defaultMetaData = new DefaultMetaData(recordModel);

        return new DefaultResult<MetaData>(defaultMetaData);
    }

    private Result<MetaData> getInputMetadataUsingStatementMetadata(PreparedStatement statement, List<String> fieldNames) throws SQLException
    {
        ParameterMetaData parameterMetaData = statement.getParameterMetaData();

        Map<String, MetaDataModel> recordModels = new HashMap<String, MetaDataModel>();
        int i = 1;
        for (String fieldName : fieldNames)
        {
            int dataType = parameterMetaData.getParameterType(i++);
            recordModels.put(fieldName, getDataTypeMetadataModel(dataType));
        }

        DefaultDefinedMapMetaDataModel recordModel = new DefaultDefinedMapMetaDataModel(recordModels);
        DefaultMetaData defaultMetaData = new DefaultMetaData(recordModel);

        return new DefaultResult<MetaData>(defaultMetaData);
    }

    private String getReferencedField(InputQueryParam inputParam)
    {
        if (inputParam.getValue() == null || !(inputParam.getValue() instanceof String))
        {
            return null;
        }
        String value = (String) inputParam.getValue();

        if (value.startsWith("#[") && value.endsWith("]"))
        {
            value = value.substring(2, value.length() - 1);

            if (value.startsWith("payload."))
            {
                value = value.substring(8);
                if (isValidIdentifier(value))
                {
                    return value;
                }
            }
            else if ((value.startsWith("payload['") && value.endsWith("']")) || (value.startsWith("payload[\"") && value.endsWith("\"]")))
            {
                value = value.substring(9, value.length() - 2);
                if (isValidIdentifier(value))
                {
                    return value;
                }
            }

        }

        return null;
    }

    private boolean isValidIdentifier(String value)
    {
        for (char c : value.toCharArray())
        {
            if (!Character.isJavaIdentifierPart(c))
            {
                return false;
            }
        }

        return true;
    }

    protected MetaDataModel getDataTypeMetadataModel(int columnTypeName)
    {
        if (dbToMetaDataType == null)
        {
            synchronized (this)
            {
                if (dbToMetaDataType == null)
                {
                    initializeDbToMetaDataType();
                }
            }
        }

        MetaDataModel metaDataModel = dbToMetaDataType.get(columnTypeName);

        if (metaDataModel != null)
        {
            return metaDataModel;
        }
        else
        {
            return new DefaultUnknownMetaDataModel();
        }
    }

    private void initializeDbToMetaDataType()
    {
        dbToMetaDataType = new HashMap<Integer, MetaDataModel>();

        dbToMetaDataType.put(Types.BIT, new DefaultSimpleMetaDataModel(DataType.BOOLEAN));
        dbToMetaDataType.put(Types.TINYINT, new DefaultSimpleMetaDataModel(DataType.BYTE));
        dbToMetaDataType.put(Types.SMALLINT, new DefaultSimpleMetaDataModel(DataType.SHORT));
        dbToMetaDataType.put(Types.INTEGER, new DefaultSimpleMetaDataModel(DataType.INTEGER));
        dbToMetaDataType.put(Types.BIGINT, new DefaultSimpleMetaDataModel(DataType.LONG));
        dbToMetaDataType.put(Types.FLOAT, new DefaultSimpleMetaDataModel(DataType.FLOAT));
        dbToMetaDataType.put(Types.REAL, new DefaultSimpleMetaDataModel(DataType.FLOAT));
        dbToMetaDataType.put(Types.DOUBLE, new DefaultSimpleMetaDataModel(DataType.DOUBLE));
        dbToMetaDataType.put(Types.NUMERIC, new DefaultSimpleMetaDataModel(DataType.DECIMAL));
        dbToMetaDataType.put(Types.DECIMAL, new DefaultSimpleMetaDataModel(DataType.DECIMAL));
        dbToMetaDataType.put(Types.CHAR, new DefaultSimpleMetaDataModel(DataType.STRING));
        dbToMetaDataType.put(Types.VARCHAR, new DefaultSimpleMetaDataModel(DataType.STRING));
        dbToMetaDataType.put(Types.LONGNVARCHAR, new DefaultSimpleMetaDataModel(DataType.STRING));

        DefaultSimpleMetaDataModel dateMetaDataModel = new DefaultSimpleMetaDataModel(DataType.DATE);
        dateMetaDataModel.setImplementationClass(Date.class.getName());
        dbToMetaDataType.put(Types.DATE, dateMetaDataModel);

        DefaultSimpleMetaDataModel timeMetaDataModel = new DefaultSimpleMetaDataModel(DataType.DATE_TIME);
        timeMetaDataModel.setImplementationClass(Time.class.getName());
        dbToMetaDataType.put(Types.TIME, timeMetaDataModel);

        DefaultSimpleMetaDataModel timestampMetaDataModel = new DefaultSimpleMetaDataModel(DataType.DATE_TIME);
        timeMetaDataModel.setImplementationClass(Timestamp.class.getName());
        dbToMetaDataType.put(Types.TIMESTAMP, timestampMetaDataModel);

        MetaDataModel binaryMetaDataModel = new DefaultListMetaDataModel(new DefaultSimpleMetaDataModel(DataType.BYTE), true);
        dbToMetaDataType.put(Types.BINARY, binaryMetaDataModel);

        MetaDataModel varBinaryMetaDataModel = new DefaultListMetaDataModel(new DefaultSimpleMetaDataModel(DataType.BYTE), true);
        dbToMetaDataType.put(Types.VARBINARY, varBinaryMetaDataModel);

        MetaDataModel longVarBinaryMetaDataModel = new DefaultListMetaDataModel(new DefaultSimpleMetaDataModel(DataType.BYTE), true);
        dbToMetaDataType.put(Types.LONGVARBINARY, longVarBinaryMetaDataModel);

        dbToMetaDataType.put(Types.NULL, new DefaultUnknownMetaDataModel());
        dbToMetaDataType.put(Types.OTHER, new DefaultUnknownMetaDataModel());
        dbToMetaDataType.put(Types.JAVA_OBJECT, new DefaultPojoMetaDataModel(Object.class));
        dbToMetaDataType.put(Types.DISTINCT, new DefaultUnknownMetaDataModel());
        dbToMetaDataType.put(Types.STRUCT, new DefaultPojoMetaDataModel(Struct.class));
        dbToMetaDataType.put(Types.ARRAY, new DefaultListMetaDataModel(new DefaultUnknownMetaDataModel(), true));
        dbToMetaDataType.put(Types.BLOB, new DefaultPojoMetaDataModel(Blob.class));
        dbToMetaDataType.put(Types.CLOB, new DefaultPojoMetaDataModel(Clob.class));
        dbToMetaDataType.put(Types.REF, new DefaultPojoMetaDataModel(Ref.class));
        dbToMetaDataType.put(Types.DATALINK, new DefaultPojoMetaDataModel(URL.class));
        dbToMetaDataType.put(Types.BOOLEAN, new DefaultSimpleMetaDataModel(DataType.BOOLEAN));
        dbToMetaDataType.put(Types.ROWID, new DefaultPojoMetaDataModel(RowId.class));
        dbToMetaDataType.put(Types.NCHAR, new DefaultSimpleMetaDataModel(DataType.STRING));
        dbToMetaDataType.put(Types.NVARCHAR, new DefaultSimpleMetaDataModel(DataType.STRING));
        dbToMetaDataType.put(Types.LONGNVARCHAR, new DefaultSimpleMetaDataModel(DataType.STRING));
        dbToMetaDataType.put(Types.NCLOB, new DefaultSimpleMetaDataModel(DataType.STRING));
        dbToMetaDataType.put(Types.SQLXML, new DefaultPojoMetaDataModel(SQLXML.class));
    }

    @Override
    public Result<MetaData> getInputMetaData()
    {
        if (query.isDynamic())
        {
            return new DefaultResult<MetaData>(null, Result.Status.FAILURE, "Cannot obtain metadata from a dynamic SQL");
        }
        else
        {
            return getMetaDataResult(query, new InputMetadataResolver());
        }
    }

    @Override
    public Result<MetaData> getOutputMetaData(MetaData metaData)
    {
        if (query.isDynamic())
        {
            Result<MetaData> staticMetadata = getStaticOutputMetadata();
            if (staticMetadata != null)
            {
                return staticMetadata;
            }
            else
            {
                return new DefaultResult<MetaData>(null, Result.Status.FAILURE, "Cannot obtain metadata from a dynamic SQL");
            }
        }
        else
        {
            return getMetaDataResult(query, new OutputMetadataResolver());
        }
    }

    private Result<MetaData> getMetaDataResult(Query query, MetadataResolver metadataResolver)
    {
        DbConnection connection = null;

        DbConfig dbConfig = dbConfigResolver.resolve(null);

        try
        {
            try
            {
                connection = dbConfig.getConnectionFactory().createConnection(TransactionalAction.NOT_SUPPORTED);
            }
            catch (SQLException e)
            {
                return new DefaultResult<MetaData>(null, Result.Status.FAILURE, e.getMessage(), FailureType.CONNECTION_FAILURE, e);
            }

            PreparedStatement preparedStatement = null;
            try
            {
                preparedStatement = connection.prepareStatement(query.getQueryTemplate().getSqlText());
                return metadataResolver.resolveMetaData(preparedStatement, query);
            }
            catch (SQLException e)
            {
                return new DefaultResult<MetaData>(null, Result.Status.FAILURE, e.getMessage(), FailureType.INVALID_CONFIGURATION, e);
            }
            finally
            {
              if (preparedStatement != null)
              {
                  try
                  {
                      preparedStatement.close();
                  }
                  catch (SQLException e)
                  {
                      if (logger.isWarnEnabled())
                      {
                          logger.warn("Could not close statement", e);
                      }
                  }
              }
            }

        }
        finally
        {
            dbConfig.getConnectionFactory().releaseConnection(connection);
        }
    }

    private interface MetadataResolver
    {

        Result<MetaData> resolveMetaData(PreparedStatement statement, Query query);
    }

    private class OutputMetadataResolver implements MetadataResolver
    {

        @Override
        public Result<MetaData> resolveMetaData(PreparedStatement statement, Query query)
        {
            return getDynamicOutputMetadata(statement);
        }
    }

    private class InputMetadataResolver implements MetadataResolver
    {

        @Override
        public Result<MetaData> resolveMetaData(PreparedStatement statement, Query query)
        {
            return getDynamicInputMetadata(statement, query);
        }
    }
}
