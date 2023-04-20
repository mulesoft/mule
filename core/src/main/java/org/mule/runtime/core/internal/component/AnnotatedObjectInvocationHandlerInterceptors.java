/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.component;

import static java.lang.Integer.toHexString;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.synchronizedMap;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleRuntimeException;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Empty;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AnnotatedObjectInvocationHandlerInterceptors {

  public static <T, A> T removeDynamicAnnotations(A annotated) {
    if (annotated instanceof DynamicallyComponent) {
      Class<?> baseClass = annotated.getClass().getSuperclass();

      Map<String, Field> fieldsByName = new HashMap<>();
      Class<?> currentClass = baseClass;
      while (currentClass != Object.class) {
        Field[] targetFields = currentClass.getDeclaredFields();

        for (Field field : targetFields) {
          if (!isStatic(field.getModifiers()) && !fieldsByName.containsKey(field.getName())) {
            fieldsByName.put(field.getName(), field);
          }
        }

        currentClass = currentClass.getSuperclass();
      }

      try {
        T base = (T) baseClass.newInstance();
        for (Field field : fieldsByName.values()) {
          boolean acc = field.isAccessible();
          field.setAccessible(true);
          try {
            field.set(base, field.get(annotated));
          } finally {
            field.setAccessible(acc);
          }
        }

        return base;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      return (T) annotated;
    }
  }

  public static class ComponentInterceptor extends AbstractComponent {

    private final Map<Method, Method> overridingMethods = synchronizedMap(new HashMap<>());

    public ComponentInterceptor(Set<Method> managedMethods) {
      for (Method method : managedMethods) {
        overridingMethods.put(method, method);
      }
    }

    @RuntimeType
    public Object intercept(@This Object obj, @Origin Method method, @AllArguments Object[] args,
                            @SuperMethod(nullIfImpossible = true) Method superMethod, @Empty Object defaultValue)
        throws Throwable {
      if (overridingMethods.containsKey(method)) {
        return overridingMethods.get(method).invoke(this, args);
      } else {
        return defaultValue;
      }
    }

    public Set<Method> getOverridingMethods() {
      return overridingMethods.keySet();
    }
  }

  public static class RemoveDynamicAnnotationsInterceptor {

    @RuntimeType
    public Object intercept(@This Object obj, @Origin Method method, @AllArguments Object[] args,
                            @SuperMethod(nullIfImpossible = true) Method superMethod, @Empty Object defaultValue)
        throws Throwable {
      return removeDynamicAnnotations(obj);
    }
  }

  public static class ToStringInterceptor {

    @RuntimeType
    public Object intercept(@This Object obj, @Origin Method method, @AllArguments Object[] args, @SuperMethod Method superMethod)
        throws Throwable {
      String base = obj.getClass().getName() + "@" + toHexString(obj.hashCode()) + "; location: ";
      if (((Component) obj).getLocation() != null) {
        return base + ((Component) obj).getLocation().getLocation();
      } else {
        return base + "(null)";
      }
    }

  }

}
