/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.common.Result;
import org.mule.common.metadata.MetaData;
import org.mule.common.metadata.OperationMetaDataEnabled;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import org.mule.module.db.internal.metadata.QueryMetadataProvider;
import org.mule.module.db.internal.metadata.NullMetadataProvider;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.util.StringUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * Base class for database message processors.
 */
public abstract class AbstractDbMessageProcessor extends AbstractInterceptingMessageProcessor implements Initialisable, InterceptingMessageProcessor, OperationMetaDataEnabled
{

    protected final DbConfigResolver dbConfigResolver;
    private final TransactionalAction transactionalAction;
    private QueryMetadataProvider queryMetadataProvider = new NullMetadataProvider();
    private String source;
    private String target;

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
            throw new MessagingException(muleEvent, e);
        }
        finally
        {
            if (connection != null && mustCloseConnection())
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

    protected void validateQueryType(QueryTemplate queryTemplate)
    {
        List<QueryType> validTypes = getValidQueryTypes();
        if (validTypes == null || !validTypes.contains(queryTemplate.getType()))
        {
            throw new IllegalArgumentException(String.format("Query type must me '%s' but was '%s'", validTypes, queryTemplate.getType()));
        }
    }

    protected abstract List<QueryType> getValidQueryTypes();
}
