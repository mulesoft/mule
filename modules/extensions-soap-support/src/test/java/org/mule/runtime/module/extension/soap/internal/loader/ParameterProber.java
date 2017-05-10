/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

class ParameterProber {

  private final String name;
  private final String defaultValue;
  private final Class type;
  private final boolean required;

  ParameterProber(String name, String defaultValue, Class type, boolean required) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.type = type;
    this.required = required;
  }

  ParameterProber(String name, Class type) {
    this(name, null, type, true);
  }

  public String getName() {
    return name;
  }

  public boolean isRequired() {
    return required;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Class getType() {
    return type;
  }
}
