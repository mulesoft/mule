/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.query;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.extension.db.api.param.StatementDefinition;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.extension.db.internal.domain.param.InOutQueryParam;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.param.OutputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.DynamicDbType;
import org.mule.extension.db.internal.domain.type.StaticDbTypeManager;
import org.mule.extension.db.internal.domain.type.UnknownDbType;
import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.extension.db.internal.resolver.param.GenericParamTypeResolverFactory;
import org.mule.extension.db.internal.resolver.param.ParamTypeResolverFactory;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.MessageFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

abstract class AbstractQueryResolver<T extends StatementDefinition> implements QueryResolver<T> {

  protected Cache<String, QueryTemplate> queryTemplates = CacheBuilder.newBuilder().build();
  private QueryTemplateParser queryTemplateParser = new SimpleQueryTemplateParser();

  @Override
  public Query resolve(T statementDefinition, DbConnector connector, DbConnection connection) {
    statementDefinition = (T) statementDefinition.resolveFromTemplate();

    checkArgument(!isBlank(statementDefinition.getSql()), "sql query cannot be blank");

    QueryTemplate queryTemplate = getQueryTemplate(connector, connection, statementDefinition);
    return new Query(queryTemplate, resolveParams(statementDefinition, queryTemplate));
  }

  protected abstract List<QueryParamValue> resolveParams(T statementDefinition, QueryTemplate template);

  protected QueryTemplate createQueryTemplate(T statementDefinition, DbConnector connector, DbConnection connection) {
    QueryTemplate queryTemplate = queryTemplateParser.parse(statementDefinition.getSql());
    if (needsParamTypeResolution(queryTemplate)) {
      Map<Integer, DbType> paramTypes = getParameterTypes(connector, connection, queryTemplate);
      queryTemplate = resolveQueryTemplate(queryTemplate, paramTypes);
    }

    return queryTemplate;
  }

  private Map<Integer, DbType> getParameterTypes(DbConnector connector, DbConnection connection, QueryTemplate queryTemplate) {
    ParamTypeResolverFactory paramTypeResolverFactory =
        new GenericParamTypeResolverFactory(createTypeManager(connector, connection));

    try {
      return paramTypeResolverFactory.create(queryTemplate).getParameterTypes(connection, queryTemplate);
    } catch (SQLException e) {
      throw new QueryResolutionException("Cannot resolve parameter types", e);
    }
  }

  private QueryTemplate getQueryTemplate(DbConnector connector, DbConnection connection, T statementDefinition) {
    try {
      return queryTemplates.get(statementDefinition.getSql(),
                                () -> createQueryTemplate(statementDefinition, connector, connection));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(MessageFactory
          .createStaticMessage("Could not resolve query: " + statementDefinition.getSql(), e));
    }
  }

  private QueryTemplate resolveQueryTemplate(QueryTemplate queryTemplate, Map<Integer, DbType> paramTypes) {
    List<QueryParam> newParams = new ArrayList<>();

    for (QueryParam originalParam : queryTemplate.getParams()) {
      DbType type = paramTypes.get(originalParam.getIndex());
      QueryParam newParam;

      if (originalParam instanceof InOutQueryParam) {
        newParam = new DefaultInOutQueryParam(originalParam.getIndex(), type, originalParam.getName(),
                                              ((InOutQueryParam) originalParam).getValue());
      } else if (originalParam instanceof InputQueryParam) {
        newParam =
            new DefaultInputQueryParam(originalParam.getIndex(), type, ((InputQueryParam) originalParam).getValue(),
                                       originalParam.getName());
      } else if (originalParam instanceof OutputQueryParam) {
        newParam = new DefaultOutputQueryParam(originalParam.getIndex(), type, originalParam.getName());
      } else {
        throw new IllegalArgumentException("Unknown parameter type: " + originalParam.getClass().getName());

      }

      newParams.add(newParam);
    }

    return new QueryTemplate(queryTemplate.getSqlText(), queryTemplate.getType(), newParams);
  }

  private boolean needsParamTypeResolution(QueryTemplate template) {
    return template.getParams().stream()
        .map(QueryParam::getType)
        .anyMatch(type -> type == UnknownDbType.getInstance() || type instanceof DynamicDbType);
  }

  private DbTypeManager createTypeManager(DbConnector connector, DbConnection connection) {
    final DbTypeManager baseTypeManager = connector.getTypeManager();
    List<DbType> vendorDataTypes = connection.getVendorDataTypes();
    if (vendorDataTypes.size() > 0) {
      return new CompositeDbTypeManager(asList(baseTypeManager, new StaticDbTypeManager(connection.getVendorDataTypes())));
    }

    return baseTypeManager;
  }

}
