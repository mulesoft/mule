/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


public class DefaultComponentParameterAst implements ComponentParameterAst {

  private final String rawValue;
  private final Supplier<ParameterModel> model;
  private final ComponentMetadataAst metadata;

  private final LazyValue<Either<String, Object>> value;

  public DefaultComponentParameterAst(String rawValue, Supplier<ParameterModel> model) {
    this(rawValue, model, null);
  }

  public DefaultComponentParameterAst(String rawValue, Supplier<ParameterModel> model, ComponentMetadataAst metadata) {
    this(rawValue, null, model, metadata);
  }

  public DefaultComponentParameterAst(ComponentAst complexValue, Supplier<ParameterModel> model, ComponentMetadataAst metadata) {
    this(null, complexValue, model, metadata);
  }

  private DefaultComponentParameterAst(String rawValue, ComponentAst complexValue, Supplier<ParameterModel> model,
                                       ComponentMetadataAst metadata) {
    this.rawValue = rawValue;
    this.model = model;
    this.metadata = metadata;

    this.value = new LazyValue<>(() -> {
      if (complexValue != null) {
        return right((Object) complexValue);
        // previous implementations were assuming that an empty string was the same as the param not being present...
      } else if (isEmpty(rawValue)) {
        final Object defaultValue = getModel().getDefaultValue();
        if (isExpression(defaultValue)) {
          return left((String) defaultValue);
        } else {
          return right(defaultValue);
        }
      } else {
        AtomicReference<String> expression = new AtomicReference<>();
        AtomicReference<Object> value = new AtomicReference<>();

        getModel().getType().accept(new MetadataTypeVisitor() {

          @Override
          public void visitObjectField(ObjectFieldType objectFieldType) {
            objectFieldType.getValue().accept(this);
          }

          @Override
          public void visitBoolean(BooleanType booleanType) {
            if (isExpression(rawValue)) {
              defaultVisit(booleanType);
            } else {
              value.set(Boolean.valueOf(rawValue));
            }
          }

          @Override
          public void visitNumber(NumberType numberType) {
            if (isExpression(rawValue)) {
              defaultVisit(numberType);
            } else {
              value.set(numberType.getAnnotation(ClassInformationAnnotation.class)
                  .map(classInfo -> {
                    if (classInfo.getClassname()
                        .equals(Integer.class.getName())) {
                      return Integer.valueOf(rawValue);
                    } else if (classInfo.getClassname()
                        .equals(Float.class.getName())) {
                      return Float.valueOf(rawValue);
                    } else if (classInfo.getClassname()
                        .equals(Long.class.getName())) {
                      return Long.valueOf(rawValue);
                    } else if (classInfo.getClassname()
                        .equals(Byte.class.getName())) {
                      return Byte.valueOf(rawValue);
                    } else if (classInfo.getClassname()
                        .equals(Short.class.getName())) {
                      return Short.valueOf(rawValue);
                    } else if (classInfo.getClassname()
                        .equals(Double.class.getName())) {
                      return Double.valueOf(rawValue);
                    } else {
                      return null;
                    }
                  })
                  .orElseGet(() -> Integer.valueOf(rawValue)));
            }
          }

          @Override
          public void visitString(StringType stringType) {
            if (isExpression(rawValue)) {
              defaultVisit(stringType);
            } else {
              value.set(rawValue);
            }
          }

          @Override
          protected void defaultVisit(MetadataType metadataType) {
            if (!getModel().getAllowedStereotypes().isEmpty() && rawValue instanceof String) {
              // For references, just return the name of the referenced object
              value.set(rawValue);
            } else if (!NOT_SUPPORTED.equals(getModel().getExpressionSupport())) {
              if (isExpression(rawValue)) {
                // For complex types that may be the result of an expression, just return the expression
                expression.set(rawValue);
              } else {
                value.set(rawValue);
              }
            }
          }
        });

        if (expression.get() != null) {
          return left(expression.get());
        } else {
          return right(value.get());
        }
      }
    });
  }

  @Override
  public <T> Either<String, T> getValue() {
    return (Either<String, T>) value.get();
  }

  @Override
  public String getRawValue() {
    return rawValue;
  }

  @Override
  public ParameterModel getModel() {
    return model.get();
  }

  @Override
  public Optional<ComponentMetadataAst> getMetadata() {
    return ofNullable(metadata);
  }

}
