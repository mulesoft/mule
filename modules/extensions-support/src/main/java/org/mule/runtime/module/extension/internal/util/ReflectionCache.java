/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;

import org.reflections.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caches results of frequently done reflection lookups.
 *
 * @since 4.1
 */
public class ReflectionCache {

  private final ConcurrentMap<Class<?>, List<FieldElement>> fieldElements = new ConcurrentHashMap<>();
  private final ConcurrentMap<Class<? extends Annotation>, ConcurrentMap<Class<?>, Optional<FieldSetter>>> fieldSetterForAnnotatedField =
      new ConcurrentHashMap<>(3, 0.9f);
  private final ConcurrentMap<Class<?>, List<Field>> fieldsByClass = new ConcurrentHashMap<>();
  private final ConcurrentMap<Class<?>, Boolean> hasDefaultConstructorsByClass = new ConcurrentHashMap<>();

  public List<FieldElement> fieldElementsFor(ParameterGroupDescriptor groupDescriptor) {
    Class<?> clazz = groupDescriptor.getType().getDeclaringClass().get();
    List<FieldElement> elements = fieldElements.get(clazz);
    // This pre-check is made in order to avoid the synchronized block in the implementation of ConcurrentHashMap
    // (https://bugs.openjdk.java.net/browse/JDK-8161372)
    if (elements == null) {
      elements = fieldElements.computeIfAbsent(clazz, cls -> groupDescriptor.getType().getFields());
    }
    return elements;
  }

  public Optional<FieldSetter> getFieldSetterForAnnotatedField(Object target, Class<? extends Annotation> annotationClass) {
    ConcurrentMap<Class<?>, Optional<FieldSetter>> cache = fieldSetterForAnnotatedField.get(annotationClass);
    // This pre-check is made in order to avoid the synchronized block in the implementation of ConcurrentHashMap
    // (https://bugs.openjdk.java.net/browse/JDK-8161372)
    if (cache == null) {
      cache = fieldSetterForAnnotatedField.computeIfAbsent(annotationClass, k -> new ConcurrentHashMap<>());
    }

    final Class<?> type = target.getClass();
    Optional<FieldSetter> setter = cache.get(type);
    // This pre-check is made in order to avoid the synchronized block in the implementation of ConcurrentHashMap
    // (https://bugs.openjdk.java.net/browse/JDK-8161372)
    if (setter == null) {
      setter = cache.computeIfAbsent(type, t -> {
        List<Field> fields = getAnnotatedFields(t, annotationClass);
        if (fields.isEmpty()) {
          return empty();
        } else if (fields.size() > 1) {
          throw new IllegalModelDefinitionException(format(
                                                           "Class '%s' has %d fields annotated with @%s. Only one field may carry that annotation",
                                                           t.getName(), fields.size(), annotationClass));
        }

        return of(new FieldSetter<>(fields.get(0)));
      });
    }
    return setter;
  }

  public List<Field> getFields(Class<?> clazz) {
    List<Field> fields = fieldsByClass.get(clazz);
    // This pre-check is made in order to avoid the synchronized block in the implementation of ConcurrentHashMap
    // (https://bugs.openjdk.java.net/browse/JDK-8161372)
    if (fields == null) {
      fields = fieldsByClass.computeIfAbsent(clazz, cls -> {
        List<Field> f = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
          f.add(field);
        }

        for (Class<?> type : ReflectionUtils.getAllSuperTypes(clazz)) {
          for (Field field : type.getDeclaredFields()) {
            f.add(field);
          }
        }

        return f;
      });
    }

    return fields;
  }

  public boolean hasDefaultConstructor(Class<?> clazz) {
    Boolean value = hasDefaultConstructorsByClass.get(clazz);
    // This pre-check is made in order to avoid the synchronized block in the implementation of ConcurrentHashMap
    // (https://bugs.openjdk.java.net/browse/JDK-8161372)
    if (value == null) {
      value = hasDefaultConstructorsByClass.computeIfAbsent(clazz, cls -> ClassUtils.getConstructor(cls, new Class[] {}) != null);
    }
    return value;
  }

}
