/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines a bulk of queryTemplates
 */
public class BulkQuery
{

    private final LinkedList<QueryTemplate> queryTemplates = new LinkedList<QueryTemplate>();

    public List<QueryTemplate> getQueryTemplates()
    {
        return Collections.unmodifiableList(queryTemplates);
    }

    /**
     * Adds a new query template to the bulk
     *
     * @param queryTemplate a non parameterized query template
     */
    public void add(QueryTemplate queryTemplate)
    {
        validateQuery(queryTemplate);

        queryTemplates.add(queryTemplate);
    }

    private void validateQuery(QueryTemplate queryTemplate)
    {
        if (!queryTemplate.getParams().isEmpty())
        {
            throw new IllegalArgumentException("Bulk query cannot contain a parameterized SQL query");
        }
    }
}
