/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.config.internal.dsl.model.DependencyNode.Type.TOP_LEVEL;
import static org.mule.runtime.config.internal.dsl.model.DependencyNode.Type.UNNAMED_TOP_LEVEL;
import org.mule.runtime.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * Represents a {@link org.mule.runtime.config.internal.model.ComponentModel} dependency.
 *
 * @since 4.1
 */
public class DependencyNode {

  private String componentName;
  private ComponentIdentifier componentIdentifier;
  private final Type type;

  public DependencyNode(String componentName, ComponentIdentifier componentIdentifier, Type type) {
    this.componentName = componentName;
    this.componentIdentifier = componentIdentifier;
    this.type = type;
  }

  public String getComponentName() {
    return componentName;
  }

  public Type getType() {
    return type;
  }

  public boolean isTopLevel() {
    return TOP_LEVEL.equals(getType());
  }

  public boolean isUnnamedTopLevel() {
    return UNNAMED_TOP_LEVEL.equals(getType());
  }

  public Optional<ComponentIdentifier> getComponentIdentifier() {
    return ofNullable(this.componentIdentifier);
  }

  public enum Type {
    TOP_LEVEL, INNER, UNNAMED_TOP_LEVEL
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

    if (componentName != null ? !componentName.equals(that.componentName) : that.componentName != null) {
      return false;
    }
    if (componentIdentifier != null ? !componentIdentifier.equals(that.componentIdentifier) : that.componentIdentifier != null) {
      return false;
    }
    return type == that.type;
  }

  @Override
  public int hashCode() {
    int result = componentName != null ? componentName.hashCode() : 0;
    result = 31 * result + (componentIdentifier != null ? componentIdentifier.hashCode() : 0);
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "DependencyNode{" +
        "componentName='" + componentName + '\'' +
        ", componentIdentifier=" + componentIdentifier +
        ", type=" + type +
        '}';
  }
}
