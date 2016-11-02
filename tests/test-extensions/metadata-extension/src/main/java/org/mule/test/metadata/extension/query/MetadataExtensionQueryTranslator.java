/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.query;

import org.mule.runtime.extension.api.dsql.Direction;
import org.mule.runtime.extension.api.dsql.EntityType;
import org.mule.runtime.extension.api.dsql.Field;
import org.mule.runtime.extension.api.dsql.QueryTranslator;
import org.mule.runtime.extension.api.dsql.Value;

import java.util.List;
import java.util.StringJoiner;

public class MetadataExtensionQueryTranslator implements QueryTranslator {

  private StringJoiner translation;

  public MetadataExtensionQueryTranslator() {
    translation = new StringJoiner(" ");
  }

  @Override
  public void translateFields(List<Field> fields) {
    StringJoiner joiner = new StringJoiner(",");
    fields.forEach(f -> joiner.add("field-" + f.getName()));
    translation.add("SELECT FIELDS:");
    translation.add(joiner.toString());
  }

  @Override
  public void translateTypes(EntityType type) {
    translation.add("FROM TYPE: " + type.getName());
  }

  @Override
  public void translateOrderByFields(List<Field> orderByFields, Direction direction) {}

  @Override
  public void translateAnd() {
    translation.add("and");
  }

  @Override
  public void translateOR() {
    translation.add("or");
  }

  @Override
  public void translateComparison(String operator, Field field, Value<?> value) {
    translation.add("field-" + field.getName() + operator + value.getValue().toString());
  }

  @Override
  public void translateBeginExpression() {
    translation.add("DO WHERE");
  }

  @Override
  public void translateInitPrecedence() {}

  @Override
  public void translateEndPrecedence() {}

  @Override
  public void translateLimit(int limit) {
    translation.add("limit:" + limit);
  }

  @Override
  public void translateOffset(int offset) {

  }

  @Override
  public String getTranslation() {
    return translation.toString();
  }

}
