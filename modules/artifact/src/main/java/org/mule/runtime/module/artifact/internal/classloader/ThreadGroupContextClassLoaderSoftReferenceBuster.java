/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import static java.lang.Class.forName;
import static org.apache.commons.lang3.reflect.FieldUtils.getField;
import static org.apache.commons.lang3.reflect.FieldUtils.readStaticField;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reference utilities to bust Thread Group Context soft references.
 * 
 * @since 4.3.0
 */
public class ThreadGroupContextClassLoaderSoftReferenceBuster {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadGroupContextClassLoaderSoftReferenceBuster.class);

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
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Class {} was not found while cleaning ThreadGroupContext on undeployment", className);
      }
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

}
