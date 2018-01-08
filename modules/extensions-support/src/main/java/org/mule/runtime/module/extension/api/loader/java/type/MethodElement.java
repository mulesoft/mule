/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.NamedObject;

import javax.lang.model.element.ExecutableElement;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * A contract for an element to be considered as a Method
 *
 * @since 4.0
 */
public interface MethodElement<T extends Type>
    extends WithParameters, WithReturnType, NamedObject, WithAnnotations, WithAlias, WithDeclaringClass, WithElement {

  /**
   * @return The represented method
   */
  Optional<Method> getMethod();

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
