/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config;

import static org.mule.runtime.core.util.Preconditions.checkState;

import javax.xml.namespace.QName;

/**
 * Unique identifier for a configuration option.
 *
 * @since 4.0
 */
public class ComponentIdentifier {

  public static QName ANNOTATION_NAME = new QName("config", "componentIdentifier");

  private String namespace;
  private String identifier;

  private ComponentIdentifier() {}

  /**
   * @return the unique identifier namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * @return the unique identifier configuration name
   */
  public String getName() {
    return identifier;
  }

  public static class Builder {

    private ComponentIdentifier componentIdentifier = new ComponentIdentifier();

    /**
     * @param namespace namespace identifier of the mule language extensions module
     * @return the builder
     */
    public Builder withNamespace(String namespace) {
      componentIdentifier.namespace = namespace;
      return this;
    }

    /**
     * @param identifier identifier unique identifier within the namespace of the language configuration extension
     * @return the builder
     */
    public Builder withName(String identifier) {
      componentIdentifier.identifier = identifier;
      return this;
    }

    public ComponentIdentifier build() {
      checkState(componentIdentifier.namespace != null, "Namespace must be not null");
      checkState(componentIdentifier.identifier != null, "Name must be not null");
      return componentIdentifier;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ComponentIdentifier that = (ComponentIdentifier) o;

    if (!namespace.equals(that.namespace)) {
      return false;
    }
    return identifier.equals(that.identifier);

  }

  @Override
  public int hashCode() {
    int result = namespace.hashCode();
    result = 31 * result + identifier.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return getNamespace().equals("mule") ? getName() : getNamespace() + ":" + getName();
  }
}
