/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.query;

import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;
import org.mule.extension.db.api.param.OutputParameter;
import org.mule.extension.db.api.param.ParameterType;
import org.mule.extension.db.api.param.StoredProcedureCall;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.DbType;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class StoredProcedureQueryResolver extends ParameterizedQueryResolver<StoredProcedureCall> {

  @Override
  protected QueryTemplate createQueryTemplate(StoredProcedureCall call, DbConnector connector, DbConnection connection) {
    QueryTemplate queryTemplate = super.createQueryTemplate(call, connector, connection);

    return new QueryTemplate(queryTemplate.getSqlText(),
                             queryTemplate.getType(),
                             resolveParamTypes(queryTemplate, call),
                             queryTemplate.isDynamic());
  }

  private List<QueryParam> resolveParamTypes(QueryTemplate queryTemplate, StoredProcedureCall call) {
    return queryTemplate.getParams().stream().map(param -> {
      String paramName = param.getName();

      Optional<Object> parameterValue = call.getInputParameter(paramName);
      if (parameterValue.isPresent()) {
        return new DefaultInputQueryParam(param.getIndex(), param.getType(), parameterValue.get(), paramName);
      }

      Optional<OutputParameter> outputParameter = call.getOutputParameter(paramName);
      if (outputParameter.isPresent()) {
        final ParameterType parameterType = outputParameter.get();
        DbType type = parameterType.getType() != null ? parameterType.getType().getDbType() : param.getType();
        return new DefaultOutputQueryParam(param.getIndex(), type, paramName);
      }

      parameterValue = call.getInOutParameter(paramName);
      if (parameterValue.isPresent()) {
        return new DefaultInOutQueryParam(param.getIndex(), param.getType(), paramName, parameterValue.get());
      }

      throw new IllegalArgumentException(format("Parameter '%s' was not bound for query '%s'", paramName, call.getSql()));
    }).collect(toCollection(LinkedList::new));
  }
}
