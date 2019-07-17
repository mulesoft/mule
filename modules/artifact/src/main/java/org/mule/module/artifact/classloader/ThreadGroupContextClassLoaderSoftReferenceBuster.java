/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.artifact.classloader;

import static java.lang.Class.forName;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Reference utilities to bust Thread Group Context soft references.
 * 
 * @since 4.3.0
 */
public class ThreadGroupContextClassLoaderSoftReferenceBuster {

  private final transient static Logger logger = LoggerFactory.getLogger(ThreadGroupContextClassLoaderSoftReferenceBuster.class);

  private static final String VALUE_FIELD = "value";

  private static final String TABLE_FIELD = "table";

  private static final String BEANS_WEAK_IDENTITY_MAP_CLASS = "java.beans.WeakIdentityMap";

  private static final String CLEAR_BEAN_INFO_CACHE_METHOD = "clearBeanInfoCache";

  private static final String THREAD_GROUP_CONTEXT_FIELD = "contexts";

  private static final String THREAD_GROUP_CONTEXT_CLASS = "java.beans.ThreadGroupContext";

  public static void bustSoftReferences(ClassLoader classloader) throws MuleSoftReferenceBusterException {
    Class<?> threadGroupClass = getClass(THREAD_GROUP_CONTEXT_CLASS);

    if (threadGroupClass != null) {
      Object contexts;
      try {
        contexts = readStaticField(threadGroupClass, THREAD_GROUP_CONTEXT_FIELD, true);

        if (contexts != null) {
          final Field tableField = getTableField();
          if (tableField != null) {
            final WeakReference[] table =
                getFieldValue(tableField, contexts);
            clearContextGroupTable(table);
          }
        }
      } catch (Exception e) {
        throw new MuleSoftReferenceBusterException(classloader, e);
      }
    }
  }

  private static void clearContextGroupTable(final WeakReference[] table)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    if (table != null) {
      Method clearBeanInfoCache = null;
      for (WeakReference entry : table) {
        if (entry != null) {
          clearBeanInfoCache = clearContext(clearBeanInfoCache, entry);
        }
      }
    }
  }

  private static Method clearContext(Method clearBeanInfoCache, WeakReference entry)
      throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    Object context = getFieldValue(entry, VALUE_FIELD);
    if (context != null) {
      if (clearBeanInfoCache == null) {
        clearBeanInfoCache = getMethod(context.getClass(), CLEAR_BEAN_INFO_CACHE_METHOD);
      }
      clearBeanInfoCache.setAccessible(true);
      clearBeanInfoCache.invoke(context);
    }
    return clearBeanInfoCache;
  }

  private static Field getTableField() {
    return getField(getClass(BEANS_WEAK_IDENTITY_MAP_CLASS), TABLE_FIELD, true);
  }

  private static Class<?> getClass(String className) {
    Class<?> threadGroupClass = null;
    try {
      threadGroupClass = forName(className);
    } catch (ClassNotFoundException e1) {
      logger.debug(String.format("Class {} was not found while cleaning ThreadGroupContext on undeployment", className));
    }
    return threadGroupClass;
  }

  private static <T> T getFieldValue(Object obj, String fieldName) throws IllegalArgumentException, IllegalAccessException {
    final Field field = getField(obj.getClass(), fieldName, true);
    return (T) getFieldValue(field, obj);
  }

  private static <T> T getFieldValue(Field field, Object obj) throws IllegalArgumentException, IllegalAccessException {
    return (T) field.get(obj);

  }

  private static Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException, SecurityException {
    return clazz.getDeclaredMethod(methodName);
  }

  public static Object readStaticField(final Class<?> cls, final String fieldName, final boolean forceAccess)
      throws IllegalAccessException {
    final Field field = getField(cls, fieldName, forceAccess);
    // already forced access above, don't repeat it here:
    return readStaticField(field, false);
  }

  public static Object readStaticField(final Field field, final boolean forceAccess) throws IllegalAccessException {
    return readField(field, (Object) null, forceAccess);
  }

  public static Object readField(final Field field, final Object target, final boolean forceAccess)
      throws IllegalAccessException {
    if (forceAccess && !field.isAccessible()) {
      field.setAccessible(true);
    }

    return field.get(target);
  }

  public static Field getField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
    for (Class<?> acls = cls; acls != null; acls = acls.getSuperclass()) {
      try {
        final Field field = acls.getDeclaredField(fieldName);
        if (!Modifier.isPublic(field.getModifiers())) {
          if (forceAccess) {
            field.setAccessible(true);
          } else {
            continue;
          }
        }
        return field;
      } catch (final NoSuchFieldException ex) {
        // ignore
      }
    }
    Field match = null;
    for (final Class<?> class1 : getAllInterfaces(cls)) {
      try {
        final Field test = class1.getField(fieldName);
        match = test;
      } catch (final NoSuchFieldException ex) {
        // ignore
      }
    }
    return match;
  }

  public static List<Class<?>> getAllInterfaces(final Class<?> cls) {
    if (cls == null) {
      return null;
    }

    final LinkedHashSet<Class<?>> interfacesFound = new LinkedHashSet<>();
    getAllInterfaces(cls, interfacesFound);

    return new ArrayList<>(interfacesFound);
  }

  /**
   * Get the interfaces for the specified class.
   *
   * @param cls the class to look up, may be {@code null}
   * @param interfacesFound the {@code Set} of interfaces for the class
   */
  private static void getAllInterfaces(Class<?> cls, final HashSet<Class<?>> interfacesFound) {
    while (cls != null) {
      final Class<?>[] interfaces = cls.getInterfaces();

      for (final Class<?> i : interfaces) {
        if (interfacesFound.add(i)) {
          getAllInterfaces(i, interfacesFound);
        }
      }
      cls = cls.getSuperclass();
    }
  }

}
