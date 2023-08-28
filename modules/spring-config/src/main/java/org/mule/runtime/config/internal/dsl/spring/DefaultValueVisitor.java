/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.config.api.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Optional;

/**
 * Visitor to acquire the default value, if any, defined for the attribute.
 *
 * @since 4.0
 */
public class DefaultValueVisitor extends AbstractAttributeDefinitionVisitor {

  private Optional<Object> defaultValue = empty();

  @Override
  public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter) {
    this.defaultValue = ofNullable(defaultValue);
  }

  public Optional<Object> getDefaultValue() {
    return defaultValue;
  }

}
