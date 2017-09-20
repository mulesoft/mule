/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.unmodifiableSet;
import static net.sf.cglib.proxy.Enhancer.registerStaticCallbacks;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.component.DynamicallyComponent;
import org.mule.runtime.core.internal.util.CompositeClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;

/**
 * Provides {@code annotations} handling logic for CGLib enhanced classes that implement {@link Component} dynamically.
 *
 * @since 4.0
 */
public class AnnotatedObjectInvocationHandler {

  private static final Set<Method> MANAGED_METHODS =
      unmodifiableSet(new HashSet<>(asList(Component.class.getDeclaredMethods())));

  /**
   * Enhances the given {@code nonAnnotatedClass} to be an implementation of {@link Component}.
   *
   * @param clazz the {@link Class} to enhance to implement {@link Component}.
   * @return the enhanced class, or the given {@code clazz} if it was already annotated.
   * @throws UnsupportedOperationException if the given {@code clazz} is <b>not</b> annotated and is declared as {@code final}.
   */
  public static <T, A extends Component> Class<A> addAnnotationsToClass(Class<T> clazz) {
    if (Component.class.isAssignableFrom(clazz)
        && asList(clazz.getMethods()).stream().anyMatch(m -> "getAnnotations".equals(m.getName()) && !m.isDefault())) {
      return (Class<A>) clazz;
    }

    if (isFinal(clazz.getModifiers())) {
      throw new UnsupportedOperationException("Class '" + clazz.getName() + "' must either not be final or implement '"
          + Component.class.getName() + "'");
    }

    Enhancer enhancer = new Enhancer();
    enhancer.setInterfaces(new Class[] {DynamicallyComponent.class});
    enhancer.setSuperclass(clazz);

    ComponentInterceptor annotatedObjectInvocationHandler = new ComponentInterceptor(MANAGED_METHODS);

    CallbackHelper callbackHelper = new CallbackHelper(clazz, new Class[] {DynamicallyComponent.class}) {

      @Override
      protected Object getCallback(Method method) {
        if (MANAGED_METHODS.contains(method) || annotatedObjectInvocationHandler.getOverridingMethods().containsKey(method)) {
          return annotatedObjectInvocationHandler;
        } else {
          Optional<Method> overridingMethod = MANAGED_METHODS.stream().filter(m -> m.getName().equals(method.getName())
              && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())).findFirst();

          if (overridingMethod.isPresent()) {
            annotatedObjectInvocationHandler.getOverridingMethods().put(method, overridingMethod.get());
            return annotatedObjectInvocationHandler;
          } else {
            return NoOp.INSTANCE;
          }
        }
      }
    };

    enhancer.setCallbackTypes(callbackHelper.getCallbackTypes());
    enhancer.setCallbackFilter(callbackHelper);

    if (Enhancer.class.getClassLoader() != clazz.getClassLoader()) {
      enhancer.setClassLoader(new CompositeClassLoader(AnnotatedObjectInvocationHandler.class.getClassLoader(),
                                                       clazz.getClassLoader()));
    }

    Class<A> annotatedClass = enhancer.createClass();
    registerStaticCallbacks(annotatedClass, callbackHelper.getCallbacks());

    return annotatedClass;
  }

  /**
   * Returns a newly built object containing the state of the given {@code annotated} object, but without any of its annotations.
   * <p>
   * This is useful when trying to use the base object in some scenarios where the object is introspected, to avoid the
   * dynamically added stuff to interfere with that introspection.
   * <p>
   * Note that there is no consistent state kept between the {@code annotated} and the returned objects. After calling this
   * method, the {@code annotated} object should be discarded (unless it is immutable, which wouldn't cause any problems)
   *
   * @param annotated the object to remove dynamic stuff from
   * @return a newly built object.
   */
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

  private static class ComponentInterceptor extends AbstractComponent implements MethodInterceptor {

    private Map<Method, Method> overridingMethods = synchronizedMap(new HashMap<>());

    public ComponentInterceptor(Set<Method> managedMethods) {
      for (Method method : managedMethods) {
        overridingMethods.put(method, method);
      }
    }

    @Override
    public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
      if (overridingMethods.containsKey(method)) {
        return overridingMethods.get(method).invoke(this, args);
      } else {
        return proxy.invokeSuper(obj, args);
      }
    }

    public Map<Method, Method> getOverridingMethods() {
      return overridingMethods;
    }

  }
}
