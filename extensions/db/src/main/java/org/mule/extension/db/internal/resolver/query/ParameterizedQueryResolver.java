/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.query;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import org.mule.extension.db.api.param.ParameterizedStatementDefinition;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;

import java.util.List;
import java.util.Optional;

public class ParameterizedQueryResolver<T extends ParameterizedStatementDefinition> extends AbstractQueryResolver<T> {

  @Override
  protected List<QueryParamValue> resolveParams(T statementDefinition, QueryTemplate template) {
    return template.getInputParams().stream()
        .map(p -> {
          final String parameterName = p.getName();

          Optional<Object> parameterValue = statementDefinition.getInputParameter(parameterName);
          if (parameterValue.isPresent()) {
            return new QueryParamValue(parameterName, parameterValue.get());
          } else {
            throw new IllegalArgumentException(format("Parameter '%s' was not bound for query '%s'",
                                                      parameterName, statementDefinition.getSql()));
          }
        }).collect(toList());
  }
}
