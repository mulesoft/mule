/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.el;

import static java.lang.String.format;
import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.database.DbConfig;
import org.mule.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.module.db.internal.resolver.database.DbConfigResolver;

import java.sql.SQLException;
import java.util.List;

/**
 * Provides a base class for DB functions that creates custom DB types.
 */
public abstract class AbstractDbFunction  implements ExpressionLanguageFunction
{

    public static final String INVALID_ARGUMENT_NUMBER = "Function '%s' must receive three parameters, but %s were provided instead";
    public static final String INVALID_DB_CONFIG_ARGUMENT = "Function '%s' must receive a String parameter as the first parameter";
    public static final String INVALID_TYPE_NAME_ARGUMENT = "Function '%s' must receive a String parameter as the second parameter";
    public static final String INVALID_STRUCT_VALUES_ARGUMENT = "Function '%s' must receive a Object[] or List as the third parameter";
    private final MuleContext muleContext;

    /**
     * Creates a new instance for a given context.
     *
     * @param muleContext context associated to the artifact using the function. Non null.
     */
    protected AbstractDbFunction(MuleContext muleContext)
    {
        checkArgument(muleContext != null, "muleContext cannot be null");
        this.muleContext = muleContext;
    }

    @Override
    public Object call(Object[] params, ExpressionLanguageContext context)
    {
        validateParams(params);
        DbConnection connection = null;

        DbConfigResolver dbConfigResolver = muleContext.getRegistry().get((String) params[0]);
        final MuleEvent muleEvent = getMuleEvent(context);
        DbConfig dbConfig = dbConfigResolver.resolve(muleEvent);

        try
        {
            connection = dbConfig.getConnectionFactory().createConnection(TransactionalAction.ALWAYS_JOIN);

            String typeName = (String) params[1];
            Object[] structValues = params[2] instanceof List? ((List) params[2]).toArray() : (Object[]) params[2];

            return createValue(connection, typeName, structValues);
        }
        catch (SQLException e)
        {
            throw new IllegalStateException(e);
        }
        finally
        {
            dbConfig.getConnectionFactory().releaseConnection(connection);
        }
    }

    /**
     * Creates the structured type value
     *
     * @param connection connection used to create the value.
     * @param typeName name of the user defined type
     * @param values values that conform the estructured type.
     * @return the structured value created from the provided arguments
     * @throws SQLException if there is any DB related error when creating the value
     */
    protected abstract Object createValue(DbConnection connection, String typeName, Object[] values) throws SQLException;

    /**
     * @return the name of the function as it appears on MEL expressions
     */
    protected abstract String getFunctionName();

    private MuleEvent getMuleEvent(ExpressionLanguageContext context)
    {
        MuleEvent event = context.getVariable(MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE);
        if (event == null)
        {
            event = RequestContext.getEvent();
        }

        checkState(event != null, "Could not obtain MuleEvent");
        return event;
    }

    private void validateParams(Object[] params)
    {
        checkArgument(params.length == 3, createInvalidArgumentCountMessage(params.length));
        checkArgument(params[0] instanceof String, createInvalidDbConfigNameArgument());
        checkArgument(params[1] instanceof String, createInvalidDbTypeMessage());
        checkArgument(params[2] instanceof Object[] || params[2] instanceof List, createInvalidStructValuesArgument());
    }

    final String createInvalidStructValuesArgument()
    {
        return format(INVALID_STRUCT_VALUES_ARGUMENT, getFunctionName());
    }

    final String createInvalidDbTypeMessage()
    {
        return format(INVALID_TYPE_NAME_ARGUMENT, getFunctionName());
    }

    final String createInvalidDbConfigNameArgument()
    {
        return format(INVALID_DB_CONFIG_ARGUMENT, getFunctionName());
    }

    final String createInvalidArgumentCountMessage(int length)
    {
        return format(INVALID_ARGUMENT_NUMBER, getFunctionName(), length);
    }
}
