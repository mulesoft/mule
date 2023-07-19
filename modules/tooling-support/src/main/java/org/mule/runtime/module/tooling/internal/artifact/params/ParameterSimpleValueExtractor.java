/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;

/**
 * {@link ParameterModel} simple value extractor. Only accepts parameters of type {@ParameterSimpleValue} otherwise an exception
 * is thrown.
 */
public class ParameterSimpleValueExtractor implements ParameterValueVisitor {

  private String value;

  public static String extractSimpleValue(ParameterValue parameterValue) {
    ParameterSimpleValueExtractor extractor = new ParameterSimpleValueExtractor();
    parameterValue.accept(extractor);
    return extractor.get();
  }

  private String get() {
    return value;
  }

  @Override
  public void visitSimpleValue(ParameterSimpleValue text) {
    this.value = text.getValue();
  }

  @Override
  public void visitListValue(ParameterListValue list) {
    throw new IllegalStateException("Only simple values expected");
  }

  @Override
  public void visitObjectValue(ParameterObjectValue objectValue) {
    throw new IllegalStateException("Only simple values expected");
  }
}
