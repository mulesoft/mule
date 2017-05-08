/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.component;

/**
 * Representation of a scripting binding.
 *
 * @since 4.0
 */
public class ScriptingProperty {

  private final String key;
  private Object value;

  public ScriptingProperty(String key) {
    this.key = key;
  }

  public Object getValue() {
    return value;
  }

  public String getKey() {
    return key;
  }

  public void setValue(Object value) {
    this.value = value;
  }

}
