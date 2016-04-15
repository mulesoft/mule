/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.parser;

import org.mule.module.db.internal.domain.query.QueryTemplate;

/**
 * Parses a SQL queries
 */
public interface QueryTemplateParser
{

    /**
     * Parses a SQL query
     *
     * @param sql non empty query to parse
     * @return a non null {@link QueryTemplate} representing the input SQL query
     * @throws QueryTemplateParsingException when there is a parsing error
     */
    QueryTemplate parse(String sql) throws QueryTemplateParsingException;
}
