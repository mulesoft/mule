/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static java.lang.String.format;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withParameters;
import static org.reflections.ReflectionUtils.withType;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.registry.InjectProcessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;

/**
 * Injects the {@link Registry} object for objects stored in the {@link TransientRegistry}.
 *
 * @since 4.0
 * @deprecated as of 3.7.0 since these are only used by {@link TransientRegistry} which is also deprecated. Use post processors
 *             for currently supported registries instead (i.e: {@link org.mule.runtime.core.config.spring.SpringRegistry})
 */
// TODO MULE-11737 Remove this
@Deprecated
public class RegistryProcessor implements InjectProcessor {

  private MuleContext context;

  public RegistryProcessor(MuleContext context) {
    this.context = context;
  }

  @Override
  public Object process(Object object) {
    for (Field field : getAllFields(object.getClass(), withAnnotation(Inject.class), withType(Registry.class))) {
      try {
        field.setAccessible(true);
        field.set(object, new DefaultRegistry(context));
      } catch (Exception e) {
        throw new RuntimeException(format("Could not inject dependency on field %s of type %s", field.getName(),
                                          object.getClass().getName()),
                                   e);
      }
    }
    for (Method method : getAllMethods(object.getClass(), withAnnotation(Inject.class), withParameters(Registry.class))) {
      try {
        method.invoke(object, new DefaultRegistry(context));
      } catch (Exception e) {
        throw new RuntimeException(format("Could not inject dependency on method %s of type %s", method.getName(),
                                          object.getClass().getName()),
                                   e);
      }
    }
    return object;
  }
}
