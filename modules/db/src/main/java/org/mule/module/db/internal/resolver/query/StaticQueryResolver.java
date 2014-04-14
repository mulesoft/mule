/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.Query;

/**
 * Resolves a query to a static value without using the current event
 */
public class StaticQueryResolver implements QueryResolver
{

    private final Query query;

    public StaticQueryResolver(Query query)
    {
        this.query = query;
    }

    @Override
    public Query resolve(DbConnection connection, MuleEvent muleEvent) throws QueryResolutionException
    {
        return query;
    }
}
