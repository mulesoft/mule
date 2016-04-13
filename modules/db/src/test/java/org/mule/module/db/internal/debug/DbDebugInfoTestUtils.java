/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.debug;

import static org.mule.module.db.internal.processor.DbDebugInfoUtils.SQL_TEXT_DEBUG_FIELD;
import static org.mule.module.db.internal.processor.DbDebugInfoUtils.TYPE_DEBUG_FIELD;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryTemplate;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;

/**
 * Provides utility methods for testing {@link FieldDebugInfo} on DB module
 */
public class DbDebugInfoTestUtils
{

    private DbDebugInfoTestUtils()
    {
    }

    /**
     * Creates a matcher to assert the debug info generated for a query
     *
     * @param name expected query name
     * @param queryTemplate query to compare with the debug info
     * @return a non null matcher
     */
    public static Matcher<FieldDebugInfo<?>> createQueryFieldDebugInfoMatcher(String name, QueryTemplate queryTemplate)
    {
        final List<Matcher<FieldDebugInfo<?>>> queryMatcher = new ArrayList<>();
        queryMatcher.add(fieldLike(SQL_TEXT_DEBUG_FIELD, String.class, queryTemplate.getSqlText()));
        queryMatcher.add(fieldLike(TYPE_DEBUG_FIELD, String.class, queryTemplate.getType().toString()));

        return objectLike(name, Query.class, queryMatcher);
    }
}
