/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.component;

import static org.mule.runtime.core.internal.util.MultiParentClassLoaderUtils.multiParentClassLoaderFor;

import static java.lang.Thread.currentThread;
import static java.lang.reflect.Modifier.isFinal;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.IMITATE_SUPER_CLASS;
import static net.bytebuddy.implementation.MethodCall.invoke;
import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.module.extension.privileged.component.ComponentAdditionalInterceptor;
import org.mule.runtime.module.extension.privileged.component.ComponentInterceptor;
import org.mule.runtime.module.extension.privileged.component.DynamicallyComponent;
import org.mule.runtime.module.extension.privileged.component.DynamicallySerializableComponent;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Provides {@code annotations} handling logic for Byte Buddy enhanced classes that implement {@link Component} dynamically.
 *
 * @since 4.0
 */
public final class AnnotatedObjectInvocationHandler {

  private static final Logger LOGGER = getLogger(AnnotatedObjectInvocationHandler.class);
  private static final ByteBuddy byteBuddy = new ByteBuddy();

  private static final Set<Method> MANAGED_METHODS =
      unmodifiableSet(new HashSet<>(asList(Component.class.getDeclaredMethods())));
  private static final Method COMPONENT_ADDITIONAL_INTERCEPTOR_SET_OBJ;

  static {
    try {
      COMPONENT_ADDITIONAL_INTERCEPTOR_SET_OBJ = ComponentAdditionalInterceptor.class.getMethod("setObj", Component.class);
    } catch (NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Enhances the given {@code nonAnnotatedClass} to be an implementation of {@link Component}.
   *
   * @param clazz the {@link Class} to enhance to implement {@link Component}.
   * @return the enhanced class, or the given {@code clazz} if it was already annotated.
   * @throws UnsupportedOperationException if the given {@code clazz} is <b>not</b> annotated and is declared as {@code final}.
   */
  public static <T, A extends Component> Class<A> addAnnotationsToClass(Class<T> clazz) {
    if (clazz.getPackageName().startsWith("java.")) {
      return (Class<A>) clazz;
    }

    if (Component.class.isAssignableFrom(clazz)
        && asList(clazz.getMethods()).stream().anyMatch(m -> "getAnnotations".equals(m.getName()) && !m.isDefault())) {
      return (Class<A>) clazz;
    }

    if (isFinal(clazz.getModifiers())) {
      throw new UnsupportedOperationException("Class '" + clazz.getName() + "' must either not be final or implement '"
          + Component.class.getName() + "'");
    }

    ClassLoader classLoader = multiParentClassLoaderFor(clazz.getClassLoader());
    final String enhancedClassName = clazz.getName() + "$ByteBuddy$withAnnotations";

    try {
      return (Class<A>) classLoader.loadClass(enhancedClassName);
    } catch (ClassNotFoundException e) {
      return doAddAnnotationsToClass(clazz, classLoader, enhancedClassName);
    }
  }

  private static <T, A extends Component> Class<A> doAddAnnotationsToClass(Class<T> clazz, ClassLoader classLoader,
                                                                           final String enhancedClassName) {
    Class dynamicInterface;
    if (Serializable.class.isAssignableFrom(clazz)) {
      try {
        clazz.getConstructor();
        dynamicInterface = DynamicallySerializableComponent.class;
      } catch (SecurityException e) {
        throw new UnsupportedOperationException("Class '" + clazz.getName() + "' cannot be enhanced for annotations ("
            + e.getMessage() + ")", e);
      } catch (NoSuchMethodException e) {
        LOGGER.warn("Class '" + clazz.getName() + "' implements Serializable but does not provide a default public constructor."
            + " The mechanism to add annotations dynamically requires a default public constructor in a Serializable class.");
        dynamicInterface = DynamicallyComponent.class;
      }
    } else {
      dynamicInterface = DynamicallyComponent.class;
    }

    ComponentInterceptor annotatedObjectInvocationHandler = new ComponentInterceptor();
    final MethodDelegation implementation = to(annotatedObjectInvocationHandler);

    DynamicType.Builder builder =
        byteBuddy.subclass(clazz, IMITATE_SUPER_CLASS).implement(dynamicInterface);
    for (Method method : MANAGED_METHODS) {
      builder = builder
          .method(named(method.getName()).and(takesArguments(method.getParameterTypes())))
          .intercept(implementation);
    }

    final ComponentAdditionalInterceptor annotatedObjectAdditionalInvocationHandler = new ComponentAdditionalInterceptor();
    builder = builder
        .name(enhancedClassName)
        .method(named("writeReplace").or(isToString().and(isDeclaredBy(Object.class))))
        .intercept(to(annotatedObjectAdditionalInvocationHandler));

    builder = builder.constructor(ElementMatchers.any())
        .intercept(SuperMethodCall.INSTANCE
            .andThen(invoke(COMPONENT_ADDITIONAL_INTERCEPTOR_SET_OBJ).on(annotatedObjectAdditionalInvocationHandler).withThis()));

    // Preload .class resource with caching disabled to avoid JarFile locking issues on Windows.
    // Without this, URLClassLoader may retain open handles to the JAR, breaking deleteDirectory() in tests.
    preloadClassResourceWithoutCaching(clazz);

    return builder.make().load(classLoader).getLoaded();
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
    return AnnotatedObjectInvocationHandlerInterceptors.removeDynamicAnnotations(annotated);
  }

  /**
   * Preloads the `.class` resource for the given class with caching disabled in the {@link URLConnection},
   * to prevent the underlying {@code JarFile} from being cached and locked by the JVM.
   * <p>
   * This is particularly important on Windows, where open {@code JarFile}s can prevent file deletion,
   * causing issues in test environments that rely on cleaning up deployed artifacts.
   * </p>
   *
   * @param clazz the class whose `.class` resource should be preloaded without caching
   */
  private static void preloadClassResourceWithoutCaching(Class<?> clazz) {
    String resourceName = clazz.getName().replace('.', '/') + ".class";
    URL resource = clazz.getClassLoader().getResource(resourceName);
    if (resource != null) {
      try {
        URLConnection conn = resource.openConnection();
        conn.setUseCaches(false);
        conn.getInputStream().close();
      } catch (IOException e) {
        LOGGER.warn("Unable to load resource " + resourceName, e);
      }
    }
  }
}
