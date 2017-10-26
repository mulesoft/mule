/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.enricher.stereotypes;

import org.mule.runtime.api.meta.model.declaration.fluent.WithStereotypesDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * {@link StereotypeResolver} implementation for java classes.
 *
 * @since 4.0
 */
class ClassStereotypeResolver extends StereotypeResolver<Type> {

  public ClassStereotypeResolver(Type annotatedElement,
                                 WithStereotypesDeclaration declaration,
                                 String namespace,
                                 StereotypeModel fallbackStereotype,
                                 Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    super(annotatedElement, declaration, namespace, fallbackStereotype, stereotypesCache);
  }

  @Override
  protected <T extends Annotation> T getAnnotation(Class<T> annotationType) {
    return annotatedElement.getAnnotation(annotationType).orElseGet(() -> {
      Class<?> declaringClass = annotatedElement.getDeclaringClass();
      if (declaringClass != null) {
        return IntrospectionUtils.getAnnotation(declaringClass, annotationType);
      }

      return null;
    });
  }

  @Override
  protected String resolveDescription() {
    return "Class '" + annotatedElement.getName() + "'";
  }
}
