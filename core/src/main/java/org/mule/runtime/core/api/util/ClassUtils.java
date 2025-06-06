/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.metadata.java.api.utils.ClassUtils.getInnerClassName;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory.getDefaultFactory;
import static org.mule.runtime.core.api.util.ExceptionUtils.tryExpecting;

import static java.lang.Boolean.getBoolean;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.ServiceLoader.load;
import static java.util.stream.StreamSupport.stream;

import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

import com.google.common.primitives.Primitives;

/**
 * Extend the Apache Commons ClassUtils to provide additional functionality.
 * <p/>
 * <p>
 * This class is useful for loading resources and classes in a fault tolerant manner that works across different applications
 * servers. The resource and classloading methods are SecurityManager friendly.
 * </p>
 */
public class ClassUtils {

  public static final Object[] NO_ARGS = new Object[] {};
  public static final Class<?>[] NO_ARGS_TYPE = new Class<?>[] {};

  private static final Map<Class<?>, Class<?>> wrapperToPrimitiveMap = new HashMap<>();
  private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);
  public static final String MULE_DESIGN_MODE = "mule.designMode";

  private static LazyValue<ClassLoaderResourceNotFoundExceptionFactory> resourceNotFoundExceptionFactoryLazyValue =
      new LazyValue(() -> {
        return stream(((Iterable<ClassLoaderResourceNotFoundExceptionFactory>) () -> load(ClassLoaderResourceNotFoundExceptionFactory.class,
                                                                                          ClassUtils.class.getClassLoader())
                                                                                              .iterator())
                                                                                                  .spliterator(),
                      false)
                          .findFirst()
                          .orElseGet(() -> getDefaultFactory());
      });

  static {
    wrapperToPrimitiveMap.put(Boolean.class, Boolean.TYPE);
    wrapperToPrimitiveMap.put(Byte.class, Byte.TYPE);
    wrapperToPrimitiveMap.put(Character.class, Character.TYPE);
    wrapperToPrimitiveMap.put(Short.class, Short.TYPE);
    wrapperToPrimitiveMap.put(Integer.class, Integer.TYPE);
    wrapperToPrimitiveMap.put(Long.class, Long.TYPE);
    wrapperToPrimitiveMap.put(Double.class, Double.TYPE);
    wrapperToPrimitiveMap.put(Float.class, Float.TYPE);
    wrapperToPrimitiveMap.put(Void.TYPE, Void.TYPE);

    Set<Class<?>> primitiveTypes = new HashSet<>(32);
    primitiveTypes.addAll(wrapperToPrimitiveMap.values());
    for (Class<?> primitiveType : primitiveTypes) {
      primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
    }
  }

  public static boolean isConcrete(Class<?> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("clazz may not be null");
    }
    return !(clazz.isInterface() || isAbstract(clazz.getModifiers()));
  }

  /**
   * Load a given resource.
   * <p/>
   * This method will try to load the resource using the following methods (in order):
   * <ul>
   * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
   * <li>From {@link Class#getClassLoader() ClassUtils.class.getClassLoader()}
   * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
   * </ul>
   *
   * @param resourceName The name of the resource to load
   * @param callingClass The Class object of the calling object
   * @return A URL pointing to the resource to load or null if the resource is not found
   */
  public static URL getResource(final String resourceName, final Class<?> callingClass) {
    URL url = AccessController.doPrivileged((PrivilegedAction<URL>) () -> {
      final ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return cl != null ? cl.getResource(resourceName) : null;
    });

    if (url == null) {
      url = AccessController
          .doPrivileged((PrivilegedAction<URL>) () -> ClassUtils.class.getClassLoader().getResource(resourceName));
    }

    if (url == null) {
      url = AccessController.doPrivileged((PrivilegedAction<URL>) () -> callingClass.getClassLoader().getResource(resourceName));
    }

    return url;
  }

  /**
   * Loads a resource from the {@code classLoader} with the given {@code resourceName}. If the resource could not be found then it
   * will fail with details information of the problem and classloading state.
   *
   * @param resourceName the resource to load.
   * @param classLoader  the classloader used to load the resource.
   * @return the {@link URL} pointing to the resource.
   */
  public static URL getResourceOrFail(final String resourceName, ClassLoader classLoader, boolean tryAbsolutePath) {
    URL url = AccessController
        .doPrivileged((PrivilegedAction<URL>) () -> classLoader != null ? classLoader.getResource(resourceName) : null);
    if (url != null) {
      return url;
    }
    if (url == null && tryAbsolutePath) {
      url = urlFromAbsolutePath(resourceName);
      if (url != null) {
        return url;
      }
    }
    if (isDesignModeEnabled()) {
      throw resourceNotFoundExceptionFactoryLazyValue.get().createResourceNotFoundException(resourceName, classLoader, true);
    } else {
      throw getDefaultFactory().createResourceNotFoundException(resourceName, classLoader, true);
    }
  }

  private static URL urlFromAbsolutePath(String uri) {
    File file = new File(uri);
    if (file.exists()) {
      try {
        return file.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return null;
  }

  public static Boolean isDesignModeEnabled() {
    return getBoolean(MULE_DESIGN_MODE);
  }

  /**
   * Same as {@link #getResourceOrFail(String, ClassLoader, boolean)} but will used the thread current classloader.
   *
   * @param resourceName the resource to load.
   * @return the {@link URL} pointing to the resource.
   */
  public static URL getResourceOrFail(final String resourceName, boolean tryAbsolutePath) {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    return getResourceOrFail(resourceName, cl, tryAbsolutePath);
  }

  @Deprecated
  public static Enumeration<URL> getResources(final String resourceName, final Class<?> callingClass) {
    return getResources(resourceName, callingClass.getClassLoader());
  }

  /**
   * Find resources with a given name.
   * <p/>
   * Resources are searched in the following order:
   * <ul>
   * <li>current thread's context classLoader</li>
   * <li>{@link ClassUtils}s class classLoader</li>
   * <li>fallbackClassLoader passed on the parameter</li>
   * </ul>
   * Search stops as soon as any of the mentioned classLoaders has found a matching resource.
   *
   * @param resourceName        resource being searched. Non empty.
   * @param fallbackClassLoader classloader used to fallback the search. Non null.
   * @return a non null {@link Enumeration} containing the found resources. Can be empty.
   */
  public static Enumeration<URL> getResources(final String resourceName, final ClassLoader fallbackClassLoader) {
    checkArgument(!isEmpty(resourceName), "ResourceName cannot be empty");
    checkArgument(fallbackClassLoader != null, "FallbackClassLoader cannot be null");

    Enumeration<URL> enumeration = AccessController.doPrivileged((PrivilegedAction<Enumeration<URL>>) () -> {
      try {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl.getResources(resourceName) : null;
      } catch (IOException e) {
        return null;
      }
    });

    if (enumeration == null) {
      enumeration = AccessController.doPrivileged((PrivilegedAction<Enumeration<URL>>) () -> {
        try {
          return ClassUtils.class.getClassLoader().getResources(resourceName);
        } catch (IOException e) {
          return null;
        }
      });
    }

    if (enumeration == null) {
      enumeration = AccessController.doPrivileged((PrivilegedAction<Enumeration<URL>>) () -> {
        try {
          return fallbackClassLoader.getResources(resourceName);
        } catch (IOException e) {
          return null;
        }
      });
    }

    return enumeration;
  }

  /**
   * Load a class with a given name.
   * <p/>
   * It will try to load the class in the following order:
   * <ul>
   * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
   * <li>Using the basic {@link Class#forName(java.lang.String) }
   * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
   * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
   * </ul>
   *
   * @param className    The name of the class to load
   * @param callingClass The Class object of the calling object
   * @return The Class instance
   * @throws ClassNotFoundException If the class cannot be found anywhere.
   */
  public static Class loadClass(final String className, final Class<?> callingClass) throws ClassNotFoundException {
    return loadClass(className, callingClass, Object.class);
  }

  /**
   * Load a class with a given name.
   * <p/>
   * It will try to load the class in the following order:
   * <ul>
   * <li>From {@link Thread#getContextClassLoader() Thread.currentThread().getContextClassLoader()}
   * <li>Using the basic {@link Class#forName(java.lang.String) }
   * <li>From {@link Class#getClassLoader() ClassLoaderUtil.class.getClassLoader()}
   * <li>From the {@link Class#getClassLoader() callingClass.getClassLoader() }
   * </ul>
   *
   * @param className    The name of the class to load
   * @param callingClass The Class object of the calling object
   * @param type         the class type to expect to load
   * @return The Class instance
   * @throws ClassNotFoundException If the class cannot be found anywhere.
   */
  public static <T extends Class> T loadClass(final String className, final Class<?> callingClass, T type)
      throws ClassNotFoundException {
    if (className.length() <= 8) {
      // Could be a primitive - likely.
      if (primitiveTypeNameMap.containsKey(className)) {
        return (T) primitiveTypeNameMap.get(className);
      }
    }

    Class<?> clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
      try {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl.loadClass(className) : null;

      } catch (ClassNotFoundException e) {
        return null;
      }
    });

    if (clazz == null) {
      clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
        try {
          return Class.forName(className);
        } catch (ClassNotFoundException e) {
          return null;
        }
      });
    }

    if (clazz == null) {
      clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
        try {
          return ClassUtils.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
          return null;
        }
      });
    }

    if (clazz == null) {
      clazz = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
        try {
          return callingClass.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
          return null;
        }
      });
    }

    if (clazz == null) {
      throw new ClassNotFoundException(className);
    }

    if (type.isAssignableFrom(clazz)) {
      return (T) clazz;
    } else {
      throw new IllegalArgumentException(String.format("Loaded class '%s' is not assignable from type '%s'", clazz.getName(),
                                                       type.getName()));
    }
  }

  /**
   * Load a class with a given name from the given classloader.
   * <p/>
   * This method must be used when the resource to load it's dependant on user configuration.
   *
   * @param className   the name of the class to load
   * @param classLoader the loader to load it from
   * @return the instance of the class
   * @throws ClassNotFoundException if the class is not available in the class loader
   */
  public static Class loadClass(final String className, final ClassLoader classLoader) throws ClassNotFoundException {
    Class<?> clazz;
    try {
      clazz = classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      try {
        clazz = classLoader.loadClass(getInnerClassName(className));
      } catch (ClassNotFoundException e2) {
        if (isDesignModeEnabled()) {
          throw resourceNotFoundExceptionFactoryLazyValue.get().createClassNotFoundException(className, classLoader);
        } else {
          throw getDefaultFactory().createClassNotFoundException(className, classLoader);
        }
      }
    }
    return clazz;
  }

  /**
   * Gets the field with the given fieldName of the TargetClass making it accessible.
   *
   * @param targetClass class to get the field from.
   * @param fieldName   the name of the field.
   * @param recursive   flag to lookup in subclasses or not
   * @return the Field object
   * @throws NoSuchFieldException when the request field does not exists.
   */
  public static Field getField(Class<?> targetClass, String fieldName, boolean recursive)
      throws NoSuchFieldException {
    Class<?> clazz = targetClass;
    Field field;
    while (!Object.class.equals(clazz)) {
      try {
        field = clazz.getDeclaredField(fieldName);
        return field;
      } catch (NoSuchFieldException e) {
        // ignore and look in superclass
        if (recursive) {
          clazz = clazz.getSuperclass();
        } else {
          break;
        }
      }
    }
    throw new NoSuchFieldException(String.format("Could not find field '%s' in class %s", fieldName,
                                                 targetClass.getName()));
  }



  /**
   * Gets a field value of a given object
   *
   * @param target    object to get the field value from
   * @param fieldName then name of the field
   * @param recursive flag to lookup in subclasses
   * @param <T>       The expected type of the field.
   * @return the value of the field for the target object
   * @throws IllegalAccessException when the field is not reachable
   * @throws NoSuchFieldException   when the field does not exist.
   * @deprecated The usage of this method is discouraged because whether it succeeds or not depends on the Java version and also
   *             in the decisions of the target class owner about whether the field is strongly encapsulated or not.
   */
  @Deprecated
  public static <T> T getFieldValue(Object target, String fieldName, boolean recursive)

      throws IllegalAccessException, NoSuchFieldException {

    Field f = getField(target.getClass(), fieldName, recursive);
    boolean isAccessible = f.isAccessible();
    try {
      f.setAccessible(true);
      return (T) f.get(target);
    } finally {
      f.setAccessible(isAccessible);
    }
  }

  /**
   * Gets a static field value of a given class
   *
   * @param targetClass the class that holds the requested static field.
   * @param fieldName   the name of the field
   * @param recursive   flag to lookup for the field in subclasses or not
   * @param <T>         the type of the field
   * @return the value of the field
   * @throws NoSuchFieldException   when the field is not found
   * @throws IllegalAccessException when the field is not reachable.
   * @deprecated The usage of this method is discouraged because whether it succeeds or not depends on the Java version and also
   *             in the decisions of the target class owner about whether the field is strongly encapsulated or not.
   */
  @Deprecated
  public static <T> T getStaticFieldValue(Class<?> targetClass, String fieldName, boolean recursive)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = getField(targetClass, fieldName, recursive);
    boolean isAccessible = field.isAccessible();
    if (!isStatic(field.getModifiers())) {
      throw new IllegalAccessException(String.format("The %s field of %s class is not static", fieldName, targetClass.getName()));
    }
    try {
      field.setAccessible(true);
      return (T) field.get(null);
    } finally {
      field.setAccessible(isAccessible);
    }
  }

  @Deprecated
  private static void removeFinalModifierFromField(Field field) throws NoSuchFieldException, IllegalAccessException {
    // Starting with Java 12 changing field modifiers is not allowed.
    // https://github.com/openjdk/jdk/commit/9c70e26c146ae4c5a2e2311948efec9bf662bb8c
    // However, we do the check against Java 17 because we only want to reference LTS versions
    if (isJavaVersionAtLeast(JAVA_17)) {
      return;
    }
    Field modifiersField = getField(Field.class, "modifiers", false);
    boolean isModifiersFieldAccessible = modifiersField.isAccessible();
    try {
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    } finally {
      modifiersField.setAccessible(isModifiersFieldAccessible);
    }
  }

  /**
   * Sets a field of an object with the given value. If this is a final field, there will be an attempt to update the value.
   * <p>
   * Notice: If the field is final and it was initialized using a constant, the value change may not be reflected in due compiler
   * optimizations. http://java.sun.com/docs/books/jls/third_edition/html/memory.html#17.5.3
   *
   * @param target    the object that holds the target field
   * @param fieldName the name of the field
   * @param value     the value to set
   * @param recursive flags to look for the field in subclasses of the target object
   * @throws IllegalAccessException the field is not reachable
   * @throws NoSuchFieldException   the field does not exist.
   * @deprecated The usage of this method is discouraged because whether it succeeds or not depends on the Java version and also
   *             in the decisions of the target class owner about whether the field is strongly encapsulated or not.
   */
  @Deprecated
  public static void setFieldValue(Object target, String fieldName, Object value, boolean recursive)
      throws IllegalAccessException, NoSuchFieldException {
    Field field = getField(target.getClass(), fieldName, recursive);
    boolean isAccessible = field.isAccessible();

    try {
      field.setAccessible(true);
      field.set(target, value);
    } finally {
      field.setAccessible(isAccessible);
    }
  }

  /**
   * Sets a static field of a given class. If this is a final field, there will be an attempt to update the value.
   * <p>
   * Notice: If the field is final and it was initialized using a constant, the value change may not be reflected due to compiler
   * optimizations. http://java.sun.com/docs/books/jls/third_edition/html/memory.html#17.5.3
   * <p>
   * Additionally, starting from Java 12, static final fields cannot be modified at all.
   *
   * @param targetClass the target class
   * @param fieldName   the name of the field
   * @param value       the value to set
   * @param recursive   flags to look for the field in subclasses or not
   * @throws NoSuchFieldException   when the field does not exist
   * @throws IllegalAccessException when the field is not reachable, or it doesn't have <em>write</em> access under the conditions
   *                                specified in {@link Field#set(Object, Object)}.
   * @deprecated The usage of this method is discouraged because whether it succeeds or not depends on the Java version and also
   *             in the decisions of the target class owner about whether the field is strongly encapsulated or not.
   */
  @Deprecated
  public static void setStaticFieldValue(Class<?> targetClass, String fieldName, Object value, boolean recursive)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = getField(targetClass, fieldName, recursive);
    boolean isAccessible = field.isAccessible();

    if (isFinal(field.getModifiers())) {
      removeFinalModifierFromField(field);
    }
    try {
      field.setAccessible(true);
      field.set(null, value);
    } finally {
      field.setAccessible(isAccessible);
    }
  }

  /**
   * Ensure that the given class is properly initialized when the argument is passed in as .class literal. This method can never
   * fail unless the bytecode is corrupted or the VM is otherwise seriously confused.
   *
   * @param clazz the Class to be initialized
   * @return the same class but initialized
   */
  public static Class<?> initializeClass(Class<?> clazz) {
    try {
      return org.apache.commons.lang3.ClassUtils.getClass(clazz.getName(), true);
    } catch (ClassNotFoundException e) {
      IllegalStateException ise = new IllegalStateException();
      ise.initCause(e);
      throw ise;
    }
  }

  public static <T> T instantiateClass(Class<? extends T> clazz, Object... constructorArgs) throws SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class<?>[] args;
    if (constructorArgs != null) {
      args = new Class[constructorArgs.length];
      for (int i = 0; i < constructorArgs.length; i++) {
        if (constructorArgs[i] == null) {
          args[i] = null;
        } else {
          args[i] = constructorArgs[i].getClass();
        }
      }
    } else {
      args = new Class[0];
    }

    // try the arguments as given
    // Constructor ctor = clazz.getConstructor(args);
    Constructor<?> ctor = getConstructor(clazz, args);

    if (ctor == null) {
      // try again but adapt value classes to primitives
      ctor = getConstructor(clazz, wrappersToPrimitives(args));
    }

    if (ctor == null) {
      StringBuilder argsString = new StringBuilder(100);
      for (Class<?> arg : args) {
        argsString.append(arg.getName()).append(", ");
      }
      throw new NoSuchMethodException("could not find constructor on class: " + clazz + ", with matching arg params: "
          + argsString);
    }

    return (T) ctor.newInstance(constructorArgs);
  }

  public static Object instantiateClass(String name, Object... constructorArgs) throws ClassNotFoundException, SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    return instantiateClass(name, constructorArgs, (ClassLoader) null);
  }

  public static Object instantiateClass(String name, Object[] constructorArgs, Class<?> callingClass)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Class<?> clazz = loadClass(name, callingClass);
    return instantiateClass(clazz, constructorArgs);
  }

  public static Object instantiateClass(String name, Object[] constructorArgs, ClassLoader classLoader)
      throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException {
    Class<?> clazz;
    if (classLoader != null) {
      clazz = loadClass(name, classLoader);
    } else {
      clazz = loadClass(name, ClassUtils.class);
    }
    if (clazz == null) {
      throw new ClassNotFoundException(name);
    }
    return instantiateClass(clazz, constructorArgs);
  }

  public static Class<?>[] getParameterTypes(Object bean, String methodName) {
    if (!methodName.startsWith("set")) {
      methodName = "set" + methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    }

    for (Method method : bean.getClass().getMethods()) {
      if (method.getName().equals(methodName)) {
        return method.getParameterTypes();
      }
    }

    return new Class[] {};
  }

  /**
   * Returns a matching method for the given name and parameters on the given class If the parameterTypes arguments is null it
   * will return the first matching method on the class.
   *
   * @param clazz          the class to find the method on
   * @param name           the method name to find
   * @param parameterTypes an array of argument types or null
   * @return the Method object or null if none was found
   */
  public static Method getMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) {
    return getMethod(clazz, name, parameterTypes, false);
  }

  public static Method getMethod(Class clazz, String name, Class[] parameterTypes, boolean acceptNulls) {
    for (Method method : clazz.getMethods()) {
      if (method.getName().equals(name)) {
        if (parameterTypes == null) {
          return method;
        } else if (compare(method.getParameterTypes(), parameterTypes, true, acceptNulls)) {
          return method;
        }
      }
    }
    return null;
  }

  public static Constructor getConstructor(Class clazz, Class[] paramTypes) {
    return getConstructor(clazz, paramTypes, false);
  }

  /**
   * Returns available constructor in the target class that as the parameters specified.
   *
   * @param clazz      the class to search
   * @param paramTypes the param types to match against
   * @param exactMatch should exact types be used (i.e. equals rather than isAssignableFrom.)
   * @return The matching constructor or null if no matching constructor is found
   */
  public static Constructor getConstructor(Class clazz, Class[] paramTypes, boolean exactMatch) {
    for (Constructor ctor : clazz.getConstructors()) {
      Class[] types = ctor.getParameterTypes();
      if (types.length == paramTypes.length) {
        int matchCount = 0;
        for (int x = 0; x < types.length; x++) {
          if (paramTypes[x] == null) {
            matchCount++;
          } else {
            if (exactMatch) {
              if (paramTypes[x].equals(types[x]) || types[x].equals(paramTypes[x])) {
                matchCount++;
              }
            } else {
              if (paramTypes[x].isAssignableFrom(types[x]) || types[x].isAssignableFrom(paramTypes[x])) {
                matchCount++;
              }
            }
          }
        }
        if (matchCount == types.length) {
          return ctor;
        }
      }
    }
    return null;
  }

  /**
   * Can be used by serice endpoints to select which service to use based on what's loaded on the classpath
   *
   * @param className    The class name to look for
   * @param currentClass the calling class
   * @return true if the class is on the path
   */
  public static boolean isClassOnPath(String className, Class currentClass) {
    try {
      return (loadClass(className, currentClass) != null);
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Used for creating an array of class types for an array or single object
   *
   * @param object single object or array. If this parameter is null or a zero length array then {@link #NO_ARGS_TYPE} is returned
   * @return an array of class types for the object
   */
  public static Class<?>[] getClassTypes(Object object) {
    if (object == null) {
      return NO_ARGS_TYPE;
    }

    Class<?>[] types;

    if (object instanceof Object[]) {
      Object[] objects = (Object[]) object;
      if (objects.length == 0) {
        return NO_ARGS_TYPE;
      }
      types = new Class[objects.length];
      for (int i = 0; i < objects.length; i++) {
        Object o = objects[i];
        if (o != null) {
          types[i] = o.getClass();
        }
      }
    } else {
      types = new Class[] {object.getClass()};
    }

    return types;
  }

  public static String getClassName(Class clazz) {
    if (clazz == null) {
      return null;
    }
    return clazz.getSimpleName();
  }

  /**
   * Returns true if the types from array c2 are assignable to the types from c1 and the arrays are the same size. If
   * matchOnObject argument is true and there is a parameter of type Object in c1 then the method returns false. If acceptNulls
   * argument is true, null values are accepted in c2.
   *
   * @param c1            parameter types array
   * @param c2            parameter types array
   * @param matchOnObject return false if there is a parameter of type Object in c1
   * @param acceptNulls   allows null parameter types in c2
   * @return true if arrays are the same size and the types assignable from c2 to c1
   */
  public static boolean compare(Class[] c1, Class[] c2, boolean matchOnObject, boolean acceptNulls) {
    if (c1.length != c2.length) {
      return false;
    }
    for (int i = 0; i < c1.length; i++) {
      if (!acceptNulls) {
        if ((c1[i] == null) || (c2[i] == null)) {
          return false;
        }
      } else {
        if (c1[i] == null) {
          return false;
        }
        if ((c2[i] == null) && (c1[i].isPrimitive())) {
          return false;
        }
        if (c2[i] == null) {
          return true;
        }
      }
      if (c1[i].equals(Object.class) && !matchOnObject) {
        return false;
      }
      if (!primitiveToWrapper(c1[i]).isAssignableFrom(primitiveToWrapper(c2[i]))) {
        return false;
      }
    }
    return true;
  }

  public static Class[] wrappersToPrimitives(Class[] wrappers) {
    if (wrappers == null) {
      return null;
    }

    if (wrappers.length == 0) {
      return wrappers;
    }

    Class[] primitives = new Class[wrappers.length];

    for (int i = 0; i < wrappers.length; i++) {
      primitives[i] = wrapperToPrimitiveMap.getOrDefault(wrappers[i], wrappers[i]);
    }

    return primitives;
  }

  /**
   * Provide a simple-to-understand class name (with access to only Java 1.4 API).
   *
   * @param clazz The class whose name we will generate
   * @return A readable name for the class
   */
  public static String getSimpleName(Class clazz) {
    if (null == clazz) {
      return "null";
    } else {
      return classNameHelper(new BufferedReader(new CharArrayReader(clazz.getName().toCharArray())));
    }
  }

  private static String classNameHelper(Reader encodedName) {
    // I did consider separating this data from the code, but I could not find a
    // solution that was as clear to read, or clearly motivated (these data are not
    // used elsewhere).

    try {
      encodedName.mark(1);
      switch (encodedName.read()) {
        case -1:
          return "null";
        case 'Z':
          return "boolean";
        case 'B':
          return "byte";
        case 'C':
          return "char";
        case 'D':
          return "double";
        case 'F':
          return "float";
        case 'I':
          return "int";
        case 'J':
          return "long";
        case 'S':
          return "short";
        case '[':
          return classNameHelper(encodedName) + "[]";
        case 'L':
          return shorten(new BufferedReader(encodedName).readLine());
        default:
          encodedName.reset();
          return shorten(new BufferedReader(encodedName).readLine());
      }
    } catch (IOException e) {
      return "unknown type: " + e.getMessage();
    }
  }

  /**
   * @param clazz A class name (with possible package and trailing semicolon)
   * @return The short name for the class
   */
  private static String shorten(String clazz) {
    if (null != clazz && clazz.endsWith(";")) {
      clazz = clazz.substring(0, clazz.length() - 1);
    }
    if (null != clazz && clazz.lastIndexOf(".") > -1) {
      clazz = clazz.substring(clazz.lastIndexOf(".") + 1, clazz.length());
    }
    return clazz;
  }

  /**
   * Simple helper for writing object equalities.
   * <p>
   * TODO Is there a better place for this?
   *
   * @param a object to compare
   * @param b object to be compared to
   * @return true if the objects are equal (value or reference), false otherwise
   */
  public static boolean equal(Object a, Object b) {
    if (null == a) {
      return null == b;
    } else {
      return null != b && a.equals(b);
    }
  }

  public static int hash(Object[] state) {
    int hash = 0;
    for (Object element : state) {
      hash = hash * 31 + (null == element ? 0 : element.hashCode());
    }
    return hash;
  }

  // this is a shorter version of the snippet from:
  // http://www.davidflanagan.com/blog/2005_06.html#000060
  // (see comments; DF's "manual" version works fine too)
  public static URL getClassPathRoot(Class clazz) {
    CodeSource cs = clazz.getProtectionDomain().getCodeSource();
    return (cs != null ? cs.getLocation() : null);
  }

  public static Class<? extends Annotation> resolveAnnotationClass(Annotation annotation) {
    if (Proxy.isProxyClass(annotation.getClass())) {
      return (Class<Annotation>) annotation.getClass().getInterfaces()[0];
    } else {
      return annotation.getClass();
    }
  }

  /**
   * Checks that {@code value} is an instance of {@code type}.
   * <p/>
   * The value that this method adds over something like {@link Class#isInstance(Object)} is that it also considers the case in
   * which {@code type} and {@code value} are evaluate by {@link #isWrapperAndPrimitivePair(Class, Class)} as {@code true}
   *
   * @param type  the {@link Class} you want to check the {@code value against}
   * @param value an instance you want to verify is instance of {@code type}
   * @param <T>   the generic type of {@code type}
   * @return {@code true} if {@code value} is an instance of {@code type} or if they are a wrapper-primitive pair. {@code false}
   *         otherwise
   */
  public static <T> boolean isInstance(Class<T> type, Object value) {
    if (value == null) {
      return false;
    }

    if (type.isInstance(value)) {
      return true;
    }

    Class<?> valueType = value.getClass();
    return isWrapperAndPrimitivePair(type, valueType);
  }

  /**
   * Checks that a wrapper-primitive relationship exists between the two types, no matter which one is the wrapper or which is the
   * primitive.
   * <p/>
   * For example, this method will return {@code true} for both the {@link Double}/{@code double} and the
   * {@code double}/{@link Double} pairs. Notice that {@code false} will be returned for the pair {@link Long}/{code int} since
   * they don't represent the same data type.
   * <p/>
   * If any of the two types is neither wrappers or primitives, it will return {@code false}
   *
   * @param type1 a {@link Class} presumed to be a wrapper or primitive type related to {@code type2}
   * @param type2 a {@link Class} presumed to be a wrapper or primitive type related to {@code type1}
   * @return {@code true} if the types are a wrapper/primitive pair referring to the same data type. {@code false} otherwise.
   */
  private static boolean isWrapperAndPrimitivePair(Class<?> type1, Class<?> type2) {
    if (isPrimitiveWrapper(type1)) {
      return type2.equals(Primitives.unwrap(type1));
    } else if (isPrimitiveWrapper(type2)) {
      return type1.equals(Primitives.unwrap(type2));
    }

    return false;
  }

  /**
   * Checks that the given {@code type} is a primitive wrapper such as {@link Double}, {@link Boolean}, {@link Integer}, etc
   *
   * @param type the {@link Class} that is presumed to be a primitive wrapper
   * @param <T>  the generic type of the argument
   * @return {@code true} if {@code type} is a primitive wrapper. {@code false} otherwise
   */
  private static <T> boolean isPrimitiveWrapper(Class<T> type) {
    return Primitives.isWrapperType(type);
  }

  /**
   * Executes the given {@code runnable} using the given {@code classLoader} as the current {@link Thread}'s context classloader.
   * <p>
   * This method guarantees that whatever the outcome, the thread's context classloader is set back to the value that it had
   * before executing this method
   *
   * @param classLoader the context {@link ClassLoader} on which the {@code runnable} should be executed
   * @param runnable    a closure
   * @deprecated since 4.3.0 on grounds of performance overhead. Handle this manually using
   *             {@link #setContextClassLoader(Thread, ClassLoader, ClassLoader)} instead
   */
  @Deprecated
  public static void withContextClassLoader(ClassLoader classLoader, Runnable runnable) {
    try {
      withContextClassLoader(classLoader, runnable, RuntimeException.class, e -> {
        throw new MuleRuntimeException(e);
      });
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Executes the given {@code callable} using the given {@code classLoader} as the current {@link Thread}'s context classloader.
   * <p>
   * This method guarantees that whatever the outcome, the thread's context classloader is set back to the value that it had
   * before executing this method.
   * <p>
   * This method is only suitable for pieces that are only executed sporadically, are not executed at high concurrency rates and
   * are not critical performance wise. This is because all the lambdas and functional abstraction it relies on are really nice
   * from a coding POV, but it does add a performance overhead.
   *
   * @param classLoader the context {@link ClassLoader} on which the {@code runnable} should be executed
   * @param callable    a {@link Callable}
   * @return the value that the {@code callable} produced
   */
  public static <T> T withContextClassLoader(ClassLoader classLoader, Callable<T> callable) {
    return withContextClassLoader(classLoader, callable, RuntimeException.class, e -> {
      throw new MuleRuntimeException(e);
    });
  }

  /**
   * Executes the given {@code runnable} using the given {@code classLoader} as the current {@link Thread}'s context classloader.
   * <p>
   * This method guarantees that whatever the outcome, the thread's context classloader is set back to the value that it had
   * before executing this method.
   * <p>
   * This method also accounts for the possibility of the {@code runnable} to throw and exception of type
   * {@code expectedExceptionType}. If that happens, then the exception is re-thrown. If the {@code runnable} throws a
   * {@link RuntimeException} of a different type, it is also re-thrown. Finally, if an exception of any other type is found, then
   * it is handled delegating into the {@code exceptionHandler} which might in turn throw another exception of
   * {@code expectedExceptionType} or return a value
   * <p>
   * This method is only suitable for pieces that are only executed sporadically, are not executed at high concurrency rates and
   * are not critical performance wise. This is because all the lambdas and functional abstraction it relies on are really nice
   * from a coding POV, but it does add a performance overhead.
   *
   * @param classLoader           the context {@link ClassLoader} on which the {@code runnable} should be executed
   * @param runnable              a {@link Runnable}
   * @param expectedExceptionType the type of exception which is expected to be thrown
   * @param exceptionHandler      a {@link ExceptionHandler} in case an unexpected exception is found instead
   * @param <T>                   the generic type of the return value
   * @param <E>                   the generic type of the expected exception
   * @throws E if the expected exception is actually thrown
   */
  public static <T, E extends Exception> void withContextClassLoader(ClassLoader classLoader, Runnable runnable,
                                                                     Class<E> expectedExceptionType,
                                                                     ExceptionHandler<T, E> exceptionHandler)
      throws E {
    final Thread currentThread = Thread.currentThread();
    final ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    if (currentClassLoader != classLoader) {
      currentThread.setContextClassLoader(classLoader);
    }
    try {
      tryExpecting(expectedExceptionType, runnable, exceptionHandler);
    } finally {
      if (currentClassLoader != classLoader) {
        currentThread.setContextClassLoader(currentClassLoader);
      }
    }
  }

  /**
   * Executes the given {@code callable} using the given {@code classLoader} as the current {@link Thread}'s context classloader.
   * <p>
   * This method guarantees that whatever the outcome, the thread's context classloader is set back to the value that it had
   * before executing this method.
   * <p>
   * This method also accounts for the possibility of the {@code callable} to throw and exception of type
   * {@code expectedExceptionType}. If that happens, then the exception is re-thrown. If the {@code callable} throws a
   * {@link RuntimeException} of a different type, it is also re-thrown. Finally, if an exception of any other type is found, then
   * it is handled delegating into the {@code exceptionHandler} which might in turn throw another exception of
   * {@code expectedExceptionType} or return a value.
   *
   * This method is only suitable for pieces that are only executed sporadically, are not executed at high concurrency rates and
   * are not critical performance wise. This is because all the lambdas and functional abstraction it relies on are really nice
   * from a coding POV, but it does add a performance overhead.
   *
   * @param classLoader           the context {@link ClassLoader} on which the {@code runnable} should be executed
   * @param callable              a {@link Callable}
   * @param expectedExceptionType the type of exception which is expected to be thrown
   * @param exceptionHandler      a {@link ExceptionHandler} in case an unexpected exception is found instead
   * @param <T>                   the generic type of the return value
   * @param <E>                   the generic type of the expected exception
   * @return a value returned by either the {@code callable} or the {@code exceptionHandler}
   * @throws E if the expected exception is actually thrown
   */
  public static <T, E extends Exception> T withContextClassLoader(ClassLoader classLoader, Callable<T> callable,
                                                                  Class<E> expectedExceptionType,
                                                                  ExceptionHandler<T, E> exceptionHandler)
      throws E {
    final Thread currentThread = Thread.currentThread();
    final ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    if (currentClassLoader != classLoader) {
      currentThread.setContextClassLoader(classLoader);
    }
    try {
      return tryExpecting(expectedExceptionType, callable, exceptionHandler);
    } finally {
      if (currentClassLoader != classLoader) {
        currentThread.setContextClassLoader(currentClassLoader);
      }
    }
  }

  /**
   * Sets {@code newClassLoader} as the context class loader for the {@code thread}, as long as said classloader is not the same
   * instance as {@code currentClassLoader}.
   * <p>
   * Since obtaining and setting the context classloader from a thread are expensive operations, the purpose of this method is to
   * avoid performing those operations when possible, which is why the two classloaders are tested not to be the same before
   * performing the set operation. For this method to make sense, {@code currentClassLoader} should actually be the current
   * context classloader from the {@code thread}.
   * <p>
   * This is how a typical use should look like:
   *
   * <pre>
   * Thread thread = Thread.currentThread();
   * ClassLoader currentClassLoader = thread.getContextClassLoader();
   * ClassLoader newClassLoader = getNewContextClassLoader(); // this one depends on your logic
   * ClassUtils.setContextClassLoader(thread, currentClassLoader, newClassLoader);
   * try {
   *   // execute your logic
   * } finally {
   *   // set things back as they were by reversing the arguments order
   *   ClassUtils.setContextClassLoader(thread, newClassLoader, currentClassLoader);
   * }
   * </pre>
   *
   * @param thread             the thread which context classloader is to be changed
   * @param currentClassLoader the thread's current context classloader
   * @param newClassLoader     the new classloader to be set
   * @since 4.3.0
   */
  public static void setContextClassLoader(Thread thread, ClassLoader currentClassLoader, ClassLoader newClassLoader) {
    if (currentClassLoader != newClassLoader) {
      thread.setContextClassLoader(newClassLoader);
    }
  }

  /**
   * Wraps the given function {@code f} so that results for a given input are cached in the given Map.
   *
   * @param f     the function to memoize
   * @param cache the map where cached values are stored
   * @return the memoized function
   */
  public static <I, O> Function<I, O> memoize(Function<I, O> f, Map<I, O> cache) {
    return input -> cache.computeIfAbsent(input, f);
  }

  /**
   * Returns the list of interfaces implemented in a given class.
   *
   * @param aClass class to analyze. Non null.
   * @return the list of interfaces implemented in the provided class and all its super classes.
   */
  public static Class<?>[] findImplementedInterfaces(Class<?> aClass) {
    checkArgument(aClass != null, "Class to analyze cannot be null");

    Class<?> currentClass = aClass;
    List<Class<?>> foundInterfaces = new LinkedList<>();
    while (currentClass != null) {
      Class<?>[] interfaces = currentClass.getInterfaces();
      Collections.addAll(foundInterfaces, interfaces);
      currentClass = currentClass.getSuperclass();
    }

    return foundInterfaces.toArray(new Class<?>[0]);
  }

  private ClassUtils() {}
}
