/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
}
