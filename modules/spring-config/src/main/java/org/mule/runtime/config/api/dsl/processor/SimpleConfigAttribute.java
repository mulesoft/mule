/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.processor;

/**
 * Represents a simple configuration attribute.
 *
 * @since 4.0
 */
public class SimpleConfigAttribute {

  private final String name;
  private final String value;
  private final boolean valueFromSchema;

  /**
   * @param name configuration attribute name as it appears in the configuration file.
   * @param value configuration value as defined in the configuration file.
   * @param valueFromSchema true if the configuration value was not explicitly defined by the user and was retrieved from the DSL
   *        schema, false otherwise.
   */
  public SimpleConfigAttribute(String name, String value, boolean valueFromSchema) {
    this.name = name;
    this.value = value;
    this.valueFromSchema = valueFromSchema;
  }

  /**
   * @return the configuration attribute name as it appears in the configuration file.
   */
  public String getName() {
    return name;
  }

  /**
   * @return configuration value as defined in the configuration file.
   */
  public String getValue() {
    return value;
  }

  /**
   * @return true if the value came from the DSL schema, false if the value comes from explicit user configuration.
   */
  public boolean isValueFromSchema() {
    return valueFromSchema;
  }
}
