/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.database;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.database.DbConfig;

/**
 * Resolves a database config evaluating expression using a given event
 */
public class DynamicDbConfigResolver implements DbConfigResolver
{

    private final String configRef;

    public DynamicDbConfigResolver(String configRef)
    {
        this.configRef = configRef;
    }

    @Override
    public DbConfig resolve(MuleEvent muleEvent)
    {
        DbConfig delegate;
        Object expressionResult = configRef;

        MuleContext muleContext = muleEvent.getMuleContext();
        if (muleContext.getExpressionManager().isExpression((String) expressionResult))
        {
            expressionResult = muleContext.getExpressionManager().evaluate((String) expressionResult, muleEvent);
        }

        if (expressionResult instanceof DbConfig)
        {
            delegate = (DbConfig) expressionResult;
        }
        else
        {
            delegate = muleContext.getRegistry().get((String) expressionResult);
        }

        if (delegate == null)
        {
            throw new UnresolvableDbConfigException("Cannot resolve dynamic JDBC config reference: " + configRef);
        }

        return delegate;
    }
}
