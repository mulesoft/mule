/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.processor;

import static org.mule.api.debug.FieldDebugInfoFactory.createFieldDebugInfo;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;

import java.util.ArrayList;
import java.util.List;

public class DbDebugInfoUtils
{

    private DbDebugInfoUtils()
    {

    }

    public static final String QUERIES_DEBUG_FIELD = "Queries";
    public static final String QUERY_DEBUG_FIELD = "Query";
    public static final String SQL_TEXT_DEBUG_FIELD = "SQL";
    public static final String TYPE_DEBUG_FIELD = "Type";
    public static final String INPUT_PARAMS_DEBUG_FIELD = "Input params";
    public static final String CONFIG_DEBUG_FIELD = "Config";
    public static final String CONNECTION_DEBUG_FIELD = "Connection";
    public static final String PARAM_DEBUG_FIELD_PREFIX = "param ";
    public static final String PARAM_SET_DEBUG_FIELD_PREFIX = "Param set ";

    /**
     * Creates debug information for a query
     *
     * @param name name given to the query
     * @param queryTemplate query template that will be debugged
     * @return
     */
    public static FieldDebugInfo createQueryFieldDebugInfo(String name, QueryTemplate queryTemplate)
    {
        final List<FieldDebugInfo<?>> query = new ArrayList<>();
        query.add(createFieldDebugInfo(SQL_TEXT_DEBUG_FIELD, String.class, queryTemplate.getSqlText()));
        query.add(createFieldDebugInfo(TYPE_DEBUG_FIELD, String.class, queryTemplate.getType().toString()));

        return createFieldDebugInfo(name, Query.class, query);
    }
}
