/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * A contract for an element from which a parameter can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface ParameterElement extends ExtensionParameter {

  /**
   * @return The represented {@link Parameter}
   */
  Optional<Parameter> getParameter();

  /**
   * {@inheritDoc}
   */
  @Override
  default Optional<? extends AnnotatedElement> getDeclaringElement() {
    Object parameter = getParameter();
    return (Optional<? extends AnnotatedElement>) parameter;
  }
}
