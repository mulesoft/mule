/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

/**
 * Represents a {@link org.mule.runtime.config.internal.model.ComponentModel} dependency.
 *
 * @since 4.1
 */
public class DependencyNode {

  private String componentName;
  private final Type type;

  public DependencyNode(String componentName, Type type) {
    this.componentName = componentName;
    this.type = type;
  }

  public String getComponentName() {
    return componentName;
  }

  public Type getType() {
    return type;
  }

  public boolean isTopLevel() {
    return Type.TOP_LEVEL.equals(getType());
  }

  public enum Type {
    TOP_LEVEL, INNER
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DependencyNode that = (DependencyNode) o;

    if (!componentName.equals(that.componentName)) {
      return false;
    }
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = componentName.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}
