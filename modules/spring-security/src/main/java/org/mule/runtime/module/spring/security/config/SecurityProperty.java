/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.config;

/**
 * Holder for the values configured in a security property within a security manager.
 *
 * @since 4.0
 */
public class SecurityProperty {

  private String name;
  private String value;

  /**
   * @param name property name
   * @param value property value
   */
  public SecurityProperty(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * @return property name
   */
  public String getName() {
    return name;
  }

  /**
   * @return property value
   */
  public String getValue() {
    return value;
  }
}
