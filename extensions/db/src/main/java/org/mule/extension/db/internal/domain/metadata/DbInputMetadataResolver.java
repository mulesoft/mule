/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.metadata;

import static java.sql.Types.VARCHAR;
import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_CONFIGURATION;
import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbInputMetadataResolver extends BaseDbMetadataResolver implements InputTypeResolver<String> {

  @Override
  public String getCategoryName() {
    return "DbCategory";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String query)
      throws MetadataResolvingException, ConnectionException {

    this.typeLoader = context.getTypeLoader();
    this.typeBuilder = context.getTypeBuilder();

    QueryTemplate queryTemplate = parseQuery(query);
    List<InputQueryParam> inputParams = queryTemplate.getInputParams();
    // No metadata when no input parameters
    if (inputParams.size() == 0) {
      return typeBuilder.nullType().build();
    }

    PreparedStatement statement = getStatement(context, queryTemplate);
    List<String> fieldNames = new ArrayList<>();
    for (InputQueryParam inputParam : inputParams) {
      String name = inputParam.getName();
      if (name == null) {
        return typeBuilder.anyType().build();
      }

      fieldNames.add(name);
    }

    try {
      return getInputMetadataUsingStatementMetadata(statement, fieldNames);
    } catch (SQLException e) {
      return getStaticInputMetadata(fieldNames);
    }
  }

  protected QueryTemplate parseQuery(String query) {
    return new SimpleQueryTemplateParser().parse(query);
  }

  protected PreparedStatement getStatement(MetadataContext context, QueryTemplate query)
      throws ConnectionException, MetadataResolvingException {
    DbConnection connection = context.<DbConnection>getConnection()
        .orElseThrow(() -> new MetadataResolvingException("A connection is required to resolve Metadata but none was provided",
                                                          INVALID_CONFIGURATION));
    PreparedStatement statement;
    try {
      statement = connection.getJdbcConnection().prepareStatement(query.getSqlText());
    } catch (SQLException e) {
      throw new MetadataResolvingException(e.getMessage(), UNKNOWN, e);
    }
    return statement;
  }

  private MetadataType getStaticInputMetadata(List<String> fieldNames) {
    Map<String, MetadataType> recordModels = new HashMap<>();

    for (String fieldName : fieldNames) {
      recordModels.put(fieldName, getDataTypeMetadataModel(VARCHAR));
    }

    ObjectTypeBuilder record = typeBuilder.objectType().id("recordModel");
    recordModels.entrySet().forEach(e -> record.addField().key(e.getKey()).value(e.getValue()));
    return record.build();
  }

  private MetadataType getInputMetadataUsingStatementMetadata(PreparedStatement statement, List<String> fieldNames)
      throws SQLException {
    ParameterMetaData parameterMetaData = statement.getParameterMetaData();

    Map<String, MetadataType> recordModels = new HashMap<>();
    int i = 1;
    for (String fieldName : fieldNames) {
      int dataType = parameterMetaData.getParameterType(i++);
      recordModels.put(fieldName, getDataTypeMetadataModel(dataType));
    }

    ObjectTypeBuilder record = typeBuilder.objectType().id("recordModel");
    recordModels.entrySet().forEach(e -> record.addField().key(e.getKey()).value(e.getValue()));
    return record.build();
  }
}
