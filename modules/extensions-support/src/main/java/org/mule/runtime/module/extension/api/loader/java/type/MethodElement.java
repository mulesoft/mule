/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.NamedObject;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.ExecutableElement;

/**
 * A contract for an element to be considered as a Method
 *
 * @since 4.0
 */
@NoImplement
public interface MethodElement<T extends Type>
    extends WithParameters, WithReturnType, NamedObject, WithAnnotations, WithAlias, WithDeclaringClass, WithElement {

  enum Scope {
    PRIVATE, PROTECTED, PUBLIC, PACKAGE
  }

  /**
   * @return The represented method
   */
  Optional<Method> getMethod();

  /**
   * @return The list of exceptions types that a method can throw
   */
  List<Type> getExceptionTypes();

  /**
   * @return The {@link OperationContainerElement} which contains the current {@link MethodElement}
   */
  T getEnclosingType();

  @Override
  Optional<ExecutableElement> getElement();

  @Override
  default MetadataType getReturnMetadataType() {
    return getMethodReturnType(this);
  }

  @Override
  default MetadataType getAttributesMetadataType() {
    return getMethodReturnAttributesType(this);
  }

  Scope getScope();
}
