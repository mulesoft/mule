/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
