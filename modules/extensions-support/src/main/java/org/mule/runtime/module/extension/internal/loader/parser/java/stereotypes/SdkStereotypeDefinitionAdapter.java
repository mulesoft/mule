/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes;

import static java.lang.String.format;
import static java.util.Objects.hash;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.util.Objects;
import java.util.Optional;

/**
 * Adapts a {@link org.mule.runtime.extension.api.stereotype.StereotypeDefinition} into a {@link StereotypeDefinition}
 *
 * @since 4.5.0
 */
public class SdkStereotypeDefinitionAdapter implements StereotypeDefinition {

  private final org.mule.runtime.extension.api.stereotype.StereotypeDefinition delegate;

  /**
   * Returns a {@link StereotypeDefinition} defined by the {@code definitionClass}
   *
   * The definition class can a subclass of either {@link StereotypeDefinition} or
   * {@link org.mule.runtime.extension.api.stereotype.StereotypeDefinition}
   *
   * @param definitionClass the class defining the stereotype
   * @return a {@link StereotypeDefinition}
   */
  public static StereotypeDefinition from(Class<?> definitionClass) {
    Object instance;
    try {
      instance = ClassUtils.instantiateClass(definitionClass);
    } catch (Exception e) {
      throw new IllegalModelDefinitionException(format(
                                                       "Cannot instantiate stereotype definition of class '%s'. %s",
                                                       definitionClass.getName(), e.getMessage()),
                                                e);
    }

    if (instance instanceof StereotypeDefinition) {
      return (StereotypeDefinition) instance;
    } else if (instance instanceof org.mule.runtime.extension.api.stereotype.StereotypeDefinition) {
      return new SdkStereotypeDefinitionAdapter((org.mule.runtime.extension.api.stereotype.StereotypeDefinition) instance);
    } else {
      throw new IllegalModelDefinitionException(format(
                                                       "Class '%s' does not represent a valid StereotypeDefinition",
                                                       definitionClass.getName()));
    }
  }

  public SdkStereotypeDefinitionAdapter(org.mule.runtime.extension.api.stereotype.StereotypeDefinition delegate) {
    this.delegate = delegate;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public String getNamespace() {
    return delegate.getNamespace();
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return delegate.getParent().map(SdkStereotypeDefinitionAdapter::new);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SdkStereotypeDefinitionAdapter that = (SdkStereotypeDefinitionAdapter) o;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return hash(delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
