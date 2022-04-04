/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
