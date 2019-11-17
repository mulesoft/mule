/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;


public class DefaultComponentParameterAst implements ComponentParameterAst {

  private final String rawValue;
  private final ComponentAst complexValue;
  private final Supplier<ParameterModel> model;
  private final ComponentMetadataAst metadata;

  public DefaultComponentParameterAst(String rawValue, Supplier<ParameterModel> model) {
    this(rawValue, model, null);
  }

  public DefaultComponentParameterAst(String rawValue, Supplier<ParameterModel> model, ComponentMetadataAst metadata) {
    this.rawValue = rawValue;
    this.complexValue = null;
    this.model = model;
    this.metadata = metadata;
  }

  public DefaultComponentParameterAst(ComponentAst complexValue, Supplier<ParameterModel> model, ComponentMetadataAst metadata) {
    this.rawValue = null;
    this.complexValue = complexValue;
    this.model = model;
    this.metadata = metadata;
  }

  @Override
  public <T> T getValue() {
    if (complexValue != null) {
      return (T) complexValue;
    } else if (isEmpty(rawValue)) {
      // previous implementations were assuming that an empty string was the same as the param not being present...
      return (T) getModel().getDefaultValue();
    } else {
      // return (T) rawValue;
      AtomicReference<T> value = new AtomicReference<>();

      getModel().getType().accept(new MetadataTypeVisitor() {

        @Override
        public void visitObjectField(ObjectFieldType objectFieldType) {
          objectFieldType.getValue().accept(this);
        }

        @Override
        public void visitBoolean(BooleanType booleanType) {
          value.set((T) Boolean.valueOf(rawValue));
        }

        @Override
        public void visitNumber(NumberType numberType) {
          value.set((T) Integer.valueOf(rawValue));
        }

        @Override
        public void visitString(StringType stringType) {
          value.set((T) rawValue);
        }

        @Override
        protected void defaultVisit(MetadataType metadataType) {
          if (getModel().getName().equals("config-ref")) {
            // For config-ref, just return the name of the referenced object
            value.set((T) rawValue);
          } else if (!NOT_SUPPORTED.equals(getModel().getExpressionSupport())) {
            // For complex types that may be the result of an expression, just return the expression
            value.set((T) rawValue);
          }
        }
      });

      if (value.get() == null) {
        System.out.println("param value not visited: " + getModel());
      }

      return value.get();
    }
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
