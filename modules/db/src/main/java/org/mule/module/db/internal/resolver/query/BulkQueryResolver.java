/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;

import org.mule.module.db.internal.domain.query.BulkQuery;

/**
 * Resolves a {@link BulkQuery} for a given {@link MuleEvent}
 */
public interface BulkQueryResolver
{

    /**
     * Resolves a bulk query in the context of a given Mule event.
     *
     * @param muleEvent used to resolve any Mule expression
     * @return bulk query resolved for the given event, null if event is null.
     */
    BulkQuery resolve(MuleEvent muleEvent);
}
