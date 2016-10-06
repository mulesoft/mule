/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el.v2;

import org.mule.runtime.api.metadata.TypedValue;

/**
 * Represents the relationship between an EL variable or function and its value.
 *
 * @since 4.0
 */
public class Binding {

  private String identifier;
  private TypedValue value;

  public Binding(String identifier, TypedValue value) {
    this.identifier = identifier;
    this.value = value;
  }

  /**
   * @return the name of the binding
   */
  public String identifier() {
    return identifier;
  }

  /**
   * @return the bindings value
   */
  public TypedValue value() {
    return value;
  }

}
