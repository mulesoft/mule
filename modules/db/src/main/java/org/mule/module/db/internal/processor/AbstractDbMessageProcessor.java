/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import static org.mule.api.debug.FieldDebugInfoFactory.createFieldDebugInfo;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.NOT_SUPPORTED;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.debug.DebugInfoProvider;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.OperationMetaDataEnabled;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.metadata.NullMetadataProvider;
import org.mule.module.db.internal.metadata.QueryMetadataProvider;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.resolver.database.UnresolvableDbConfigException;
import org.mule.module.db.internal.result.statement.StatementStreamingResultSetCloser;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for database message processors.
 */
public abstract class AbstractDbMessageProcessor extends AbstractInterceptingMessageProcessor implements Initialisable, InterceptingMessageProcessor, OperationMetaDataEnabled, DebugInfoProvider
{

    protected final DbConfigResolver dbConfigResolver;
    private final TransactionalAction transactionalAction;
    private QueryMetadataProvider queryMetadataProvider = new NullMetadataProvider();
    private String source;
    private String target;
    private StatementStreamingResultSetCloser streamingResultSetCloser;

    public AbstractDbMessageProcessor(DbConfigResolver dbConfigResolver, TransactionalAction transactionalAction)
    {
        this.dbConfigResolver = dbConfigResolver;
        this.transactionalAction = transactionalAction;
    }

    @Override
    public MuleEvent process(MuleEvent muleEvent) throws MuleException
    {
        DbConnection connection = null;

        DbConfig dbConfig = dbConfigResolver.resolve(muleEvent);

        try
        {
            connection = dbConfig.getConnectionFactory().createConnection(transactionalAction);
        }
        catch (SQLException e)
        {
            throw new DbConnectionException(e, dbConfig);
        }

        try
        {
            Object result = executeQuery(connection, muleEvent);

            if (mustCloseConnection())
            {
                try
                {
                    dbConfig.getConnectionFactory().releaseConnection(connection);
                }
                finally
                {
                    connection = null;
                }
            }

            if (target == null || "".equals(target) || "#[payload]".equals(target))
            {
                muleEvent.getMessage().setPayload(result);
            }
            else
            {
                muleContext.getExpressionManager().enrich(target, muleEvent, result);
            }

            return processNext(muleEvent);
        }
        catch (SQLException e)
        {
            // Close all other streaming ResultSets that remain open from the current connection.
            streamingResultSetCloser.closeResultSets(connection);
            throw new MessagingException(muleEvent, e, this);
        }
        finally
        {
            if (mustCloseConnection())
            {
                dbConfig.getConnectionFactory().releaseConnection(connection);
            }
        }
    }

    protected boolean mustCloseConnection()
    {
        return true;
    }

    protected MuleEvent resolveSource(MuleEvent muleEvent)
    {
        MuleEvent eventToUse = muleEvent;

        if (!StringUtils.isEmpty(source) && !("#[payload]".equals(source)))
        {
            Object payload = muleContext.getExpressionManager().evaluate(source, muleEvent);
            MuleMessage itemMessage = new DefaultMuleMessage(payload, muleContext);
            eventToUse = new DefaultMuleEvent(itemMessage, muleEvent);
        }
        return eventToUse;
    }

    protected abstract Object executeQuery(DbConnection connection, MuleEvent muleEvent) throws SQLException;

    @Override
    public void initialise() throws InitialisationException
    {
    }

    public void setQueryMetadataProvider(QueryMetadataProvider queryMetadataProvider)
    {
        this.queryMetadataProvider = queryMetadataProvider;
    }

    public QueryMetadataProvider getQueryMetadataProvider()
    {
        return queryMetadataProvider;
    }

    @Override
    public Result<MetaData> getOutputMetaData(MetaData metaData)
    {
        return queryMetadataProvider.getOutputMetaData(metaData);
    }

    @Override
    public Result<MetaData> getInputMetaData()
    {
        return queryMetadataProvider.getInputMetaData();
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public void setStatementStreamingResultSetCloser(StatementStreamingResultSetCloser streamingResultSetCloser)
    {
        this.streamingResultSetCloser = streamingResultSetCloser;
    }

    protected void validateQueryType(QueryTemplate queryTemplate)
    {
        List<QueryType> validTypes = getValidQueryTypes();
        if (validTypes == null || !validTypes.contains(queryTemplate.getType()))
        {
            throw new IllegalArgumentException(String.format("Query type must be one of '%s' but was '%s'", validTypes, queryTemplate.getType()));
        }
    }

    @Override
    public List<FieldDebugInfo<?>> getDebugInfo(MuleEvent muleEvent)
    {
        List<FieldDebugInfo<?>> debugInfo = new ArrayList<>();

        DbConfig dbConfig;
        try
        {
            dbConfig = dbConfigResolver.resolve(muleEvent);
        }
        catch (UnresolvableDbConfigException e)
        {
            debugInfo.add(createFieldDebugInfo(DbDebugInfoUtils.CONFIG_DEBUG_FIELD, DbConfig.class, e));
            return debugInfo;
        }

        DbConnection connection;
        try
        {
            connection = dbConfig.getConnectionFactory().createConnection(NOT_SUPPORTED);
        }
        catch (SQLException e)
        {
            debugInfo.add(createFieldDebugInfo(DbDebugInfoUtils.CONNECTION_DEBUG_FIELD, DbConnection.class, e));
            return debugInfo;
        }

        try
        {
            debugInfo = getMessageProcessorDebugInfo(connection, muleEvent);

            try
            {
                dbConfig.getConnectionFactory().releaseConnection(connection);
            }
            finally
            {
                connection = null;
            }

            return debugInfo;
        }
        finally
        {
            if (connection != null && mustCloseConnection())
            {
                dbConfig.getConnectionFactory().releaseConnection(connection);
            }
        }
    }

    protected abstract List<FieldDebugInfo<?>> getMessageProcessorDebugInfo(DbConnection connection, MuleEvent muleEvent);

    protected abstract List<QueryType> getValidQueryTypes();

}
