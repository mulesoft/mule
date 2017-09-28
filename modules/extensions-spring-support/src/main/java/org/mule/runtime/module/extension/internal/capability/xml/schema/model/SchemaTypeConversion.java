/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.capability.xml.schema.model;

import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_BOOLEAN;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_DATE_TIME;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_DECIMAL;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_DOUBLE;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_INTEGER;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_LIST;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_LONG;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_MAP;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.EXPRESSION_STRING;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.STRING;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_BOOLEAN;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_DATE_TIME;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_DECIMAL;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_INT;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_LONG;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_MAP;
import static org.mule.runtime.config.internal.dsl.SchemaConstants.SUBSTITUTABLE_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.acceptsExpressions;
import org.mule.metadata.api.annotation.IntAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.util.Reference;

import javax.xml.namespace.QName;

public final class SchemaTypeConversion {

  public static QName convertType(final MetadataType type, ExpressionSupport expressionSupport) {
    final boolean dynamic = acceptsExpressions(expressionSupport);
    final Reference<QName> qName = new Reference<>(null);
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitBoolean(BooleanType booleanType) {
        qName.set(dynamic ? EXPRESSION_BOOLEAN : SUBSTITUTABLE_BOOLEAN);
      }

      @Override
      public void visitNumber(NumberType numberType) {
        if (getId(numberType).isPresent()) {
          Class<Number> type = JavaTypeUtils.getType(numberType);
          if (anyOf(type, Integer.class, int.class)) {
            qName.set(dynamic ? EXPRESSION_INTEGER : SUBSTITUTABLE_INT);
          } else if (anyOf(type, Double.class, double.class)) {
            qName.set(dynamic ? EXPRESSION_DOUBLE : SUBSTITUTABLE_DECIMAL);
          } else if (anyOf(type, Long.class, long.class)) {
            qName.set(dynamic ? EXPRESSION_LONG : SUBSTITUTABLE_LONG);
          } else {
            qName.set(dynamic ? EXPRESSION_DECIMAL : SUBSTITUTABLE_DECIMAL);
          }
        } else {
          if (numberType.getAnnotation(IntAnnotation.class).isPresent()) {
            qName.set(dynamic ? EXPRESSION_INTEGER : SUBSTITUTABLE_INT);
          } else {
            qName.set(dynamic ? EXPRESSION_DECIMAL : SUBSTITUTABLE_DECIMAL);
          }
        }
      }

      @Override
      public void visitString(StringType stringType) {
        qName.set(dynamic ? EXPRESSION_STRING : STRING);
      }

      @Override
      public void visitDateTime(DateTimeType dateTimeType) {
        onDate();
      }

      @Override
      public void visitDate(DateType dateType) {
        onDate();
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        qName.set(dynamic ? EXPRESSION_LIST : SUBSTITUTABLE_NAME);
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (isMap(objectType)) {
          qName.set(dynamic ? EXPRESSION_MAP : SUBSTITUTABLE_MAP);
        } else {
          defaultVisit(objectType);
        }
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        qName.set(STRING);
      }

      private void onDate() {
        qName.set(dynamic ? EXPRESSION_DATE_TIME : SUBSTITUTABLE_DATE_TIME);
      }

      private boolean anyOf(Class<Number> type, Class<?>... targets) {
        for (Class<?> target : targets) {
          if (type.equals(target)) {
            return true;
          }
        }

        return false;
      }

    });

    return qName.get();
  }
}
