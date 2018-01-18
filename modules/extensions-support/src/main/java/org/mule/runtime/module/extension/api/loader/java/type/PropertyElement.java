/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

/**
 * Describes a java bean property
 *
 * @since 4.1
 */
@NoImplement
public interface PropertyElement extends WithType, WithName {

  /**
   * @return The accessibility level of this property
   */
  Accessibility getAccess();

  enum Accessibility {
    READ_ONLY, WRITE_ONLY, READ_WRITE
  }

  static PropertyElementBuilder builder() {
    return new PropertyElementBuilder();
  }

  /**
   * Default {@link PropertyElement} implementation
   *
   * @since 4.1
   */
  final class DefaultPropertyElement implements PropertyElement {

    private final Type type;
    private final String name;
    private final Accessibility accessibility;

    private DefaultPropertyElement(Type type, String name, Accessibility accessibility) {
      this.type = type;
      this.name = name;
      this.accessibility = accessibility;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
      return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType() {
      return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Accessibility getAccess() {
      return accessibility;
    }

  }

  class PropertyElementBuilder {

    private Type type;
    private String name;
    private Accessibility accessibility;

    public PropertyElementBuilder type(Type type) {
      this.type = type;
      return this;
    }

    public PropertyElementBuilder name(String name) {
      this.name = name;
      return this;
    }

    public PropertyElementBuilder accessibility(Accessibility accessibility) {
      this.accessibility = accessibility;
      return this;
    }

    public PropertyElement build() {
      return new DefaultPropertyElement(type, name, accessibility);
    }

  }
}
