/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.config.params;

import static java.util.stream.Collectors.toList;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.app.declaration.api.fluent.SimpleValueType;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class ParameterExtractor implements ParameterValueVisitor {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private Object value;

  public static <T> T extractValue(ParameterValue parameterValue, Class<T> type) {
    return objectMapper.convertValue(extractValue(parameterValue), type);
  }

  public static Object extractValue(ParameterValue parameterValue) {
    final ParameterExtractor extractor = new ParameterExtractor();
    parameterValue.accept(extractor);
    return extractor.get();
  }

  private ParameterExtractor() {}

  @Override
  public void visitSimpleValue(ParameterSimpleValue text) {
    SimpleValueType valueType = text.getType();
    String value = text.getValue();
    if (valueType != null) {
      switch (valueType) {
        case BOOLEAN:
          this.value = Boolean.valueOf(value);
          break;
        case NUMBER:
          this.value = Integer.valueOf(value);
          break;
        default:
          this.value = value;
          break;
      }
    } else {
      this.value = value;
    }
  }

  @Override
  public void visitListValue(ParameterListValue list) {
    this.value = list.getValues().stream().map(ParameterExtractor::extractValue).collect(toList());
  }

  @Override
  public void visitObjectValue(ParameterObjectValue objectValue) {
    final Map<String, Object> parametersMap = new HashMap<>();
    objectValue.getParameters().forEach((k, v) -> parametersMap.put(k, extractValue(v)));
    this.value = parametersMap;
  }

  private Object get() {
    return value;
  }

}
