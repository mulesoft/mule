/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.query;

import org.mule.extension.db.api.param.BulkQueryDefinition;
import org.mule.extension.db.api.param.ParameterType;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.BulkQuery;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.runtime.core.api.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves a {@link BulkQuery} for a given {@link Event}
 */
public class BulkQueryResolver extends AbstractQueryResolver<BulkQueryDefinition> {

  @Override
  public Query resolve(BulkQueryDefinition definition, DbConnector connector, DbConnection connection) {
    Query query = super.resolve(definition, connector, connection);
    List<QueryParam> queryParams = new LinkedList<>();

    final QueryTemplate queryTemplate = query.getQueryTemplate();
    queryTemplate.getParams().forEach(inputParam -> {
      if (inputParam instanceof InputQueryParam) {
        String paramName = inputParam.getName();
        Optional<ParameterType> parameterType = definition.getParameterType(paramName);
        if (parameterType.isPresent()) {
          queryParams
              .add(new DefaultInputQueryParam(inputParam.getIndex(), parameterType.get().getDbType(), null, paramName));
          return;
        }
      }

      queryParams.add(inputParam);
    });

    return new Query(new QueryTemplate(queryTemplate.getSqlText(),
                                       queryTemplate.getType(),
                                       queryParams,
                                       queryTemplate.isDynamic()));
  }

  @Override
  protected List<QueryParamValue> resolveParams(BulkQueryDefinition statementDefinition, QueryTemplate template) {
    return new LinkedList<>();
  }
}
