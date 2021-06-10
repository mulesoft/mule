/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.core.api.el.ExpressionManager;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


public class ParameterExtractor implements ParameterValueVisitor {

  private static final ObjectMapper objectMapper;
  static {
    // This was added to handle complex parameters and transforming from a Map<String, Object>
    // to the actual object of type defined my the model.
    // TODO: CMTS-108
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new DateTimeModule());
    objectMapper.setTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
  }

  private Object value;

  public static Object extractValue(ParameterValue parameterValue, Class<?> type) {
    Object value = extractValue(parameterValue);
    if (isExpression(value) && value instanceof String) {
      return value;
    }
    return objectMapper.convertValue(value, type);
  }

  private static Object extractValue(ParameterValue parameterValue) {
    final ParameterExtractor extractor = new ParameterExtractor();
    parameterValue.accept(extractor);
    return extractor.get();
  }

  private ParameterExtractor() {}

  @Override
  public void visitSimpleValue(ParameterSimpleValue text) {
    this.value = text.getValue();
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
