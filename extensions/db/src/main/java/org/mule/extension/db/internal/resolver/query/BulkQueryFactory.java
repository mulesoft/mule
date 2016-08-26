/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.query;

import org.mule.extension.db.internal.domain.query.BulkQuery;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.QueryTemplateParser;

/**
 * Base class for {@link BulkQueryResolver} implementations
 */
public abstract class BulkQueryFactory {

  private static final String BULK_QUERY_SEPARATOR = ";[\\r\\n]+";

  private final QueryTemplateParser parser;

  public BulkQueryFactory(QueryTemplateParser queryTemplateParser) {
    this.parser = queryTemplateParser;
  }

  public BulkQuery resolve() {

    BulkQuery bulkQuery = createBulkQuery();

    if (bulkQuery.getQueryTemplates().size() == 0) {
      throw new QueryResolutionException("There are no queries on the resolved query script: " + toString());
    }

    return bulkQuery;
  }

  protected abstract String resolveBulkQueries();

  protected BulkQuery createBulkQuery() {
    String queries = resolveBulkQueries();

    BulkQuery bulkQuery = new BulkQuery();

    String[] splitQueries = queries.split(BULK_QUERY_SEPARATOR);
    for (String sql : splitQueries) {
      if ("".equals(sql.trim())) {
        continue;
      }

      QueryTemplate queryTemplate = parser.parse(sql);
      bulkQuery.add(queryTemplate);
    }

    return bulkQuery;
  }
}
