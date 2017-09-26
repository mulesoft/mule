/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.registry;

import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.internal.registry.TransientRegistry;
import org.mule.runtime.core.privileged.util.annotation.AnnotationMetaData;
import org.mule.runtime.core.privileged.util.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * This registry processor will check for objects that have JSR-250 lifecycle annotations defined and validates that the following
 * conditions are met (according to the JSR-250 spec)
 * <ol>
 * <li>The method MUST NOT have any parameters except in the case of EJB interceptors in which case it takes an InvocationContext
 * object as defined by the EJB specification. Note that Mule is not an EJB container so the EJB case is ignored in Mule.</li>
 * <li>The return type of the method MUST be void.</li>
 * <li>The method MUST NOT throw a checked exception.</li>
 * <li>The method on which PostConstruct is applied MAY be public, protected, package private or private.</li>
 * <li>The method MUST NOT be static except for the application client.</li>
 * <li>The method MAY be final or non-final, except in the case of EJBs where it MUST be non-final. Note that Mule is not an EJB
 * container so the EJB case is ignored in Mule.</li>
 * </ol>
 *
 * @deprecated as of 3.7.0 since these are only used by {@link TransientRegistry} which is also
 *             deprecated. Use post processors for currently supported registries instead (i.e:
 *             {@link org.mule.runtime.core.config.spring.SpringRegistry})
 */
@Deprecated
public class JSR250ValidatorProcessor implements InjectProcessor {

  public Object process(Object object) {
    List<AnnotationMetaData> annos = AnnotationUtils.getMethodAnnotations(object.getClass(), PostConstruct.class);
    if (annos.size() > 1) {
      throw new IllegalArgumentException(CoreMessages.objectHasMoreThanOnePostConstructAnnotation(object.getClass())
          .getMessage());
    } else if (annos.size() == 1) {
      validateLifecycleMethod((Method) annos.get(0).getMember());
    }

    annos = AnnotationUtils.getMethodAnnotations(object.getClass(), PreDestroy.class);
    if (annos.size() > 1) {
      throw new IllegalArgumentException(CoreMessages.objectHasMoreThanOnePreDestroyAnnotation(object.getClass()).getMessage());
    } else if (annos.size() == 1) {
      validateLifecycleMethod((Method) annos.get(0).getMember());
    }

    return object;
  }

  public final void validateLifecycleMethod(Method method) {
    if (method.getParameterTypes().length != 0) {
      throw new IllegalArgumentException(CoreMessages.lifecycleMethodNotVoidOrHasParams(method).getMessage());
    }

    if (!method.getReturnType().equals(Void.TYPE)) {
      throw new IllegalArgumentException(CoreMessages.lifecycleMethodNotVoidOrHasParams(method).getMessage());
    }

    if (Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException(CoreMessages.lifecycleMethodCannotBeStatic(method).getMessage());
    }

    for (Class<?> aClass : method.getExceptionTypes()) {
      if (!RuntimeException.class.isAssignableFrom(aClass)) {
        throw new IllegalArgumentException(CoreMessages.lifecycleMethodCannotThrowChecked(method).getMessage());
      }
    }
  }
}
