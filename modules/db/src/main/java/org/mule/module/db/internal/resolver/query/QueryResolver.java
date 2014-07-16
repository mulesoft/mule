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
 * Resolves a {@link Query} for a given {@link MuleEvent}
 */
public interface QueryResolver
{

    /**
     * Resolves a query in the context of a given Mule event.
     *
     * @param connection connection to the database. not null
     * @param muleEvent used to resolve any Mule expression. Not null
     * @return query resolved for the given event
     */
    Query resolve(DbConnection connection, MuleEvent muleEvent) throws QueryResolutionException;
}
