/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

/**
 * A generic contract for any kind of component that is based on a Class
 *
 * @since 4.0
 */
public interface Type extends WithAnnotations, WithName, WithAlias, WithDeclaringClass {

  /**
   * @return A list of {@link FieldElement} that represent the list of {@link Field} that the {@link Type} declares
   */
  List<FieldElement> getFields();

  /**
   * @param annotations classes that the fields of this type should be annotated with
   * @return A list of {@link FieldElement} that represent the list of {@link Field} that the {@link Type} declares and are
   * annotated with the given annotation
   */
  List<FieldElement> getAnnotatedFields(Class<? extends Annotation>... annotations);

  /**
   * Checks the assignability of the current type from the given class
   *
   * @param clazz The class to check
   * @return a boolean indicating whether the type is assignable or not from the given class
   * @since 4.1
   */
  boolean isAssignableFrom(Class<?> clazz);

  /**
   * Checks the assignability of the current type to the given class
   *
   * @param clazz The class to check
   * @return a boolean indicating whether the type is assignable or not to the given class
   * @since 4.1
   */
  boolean isAssignableTo(Class<?> clazz);

  /**
   * @return The generics for the current type.
   * @since 4.1
   */
  List<GenericInfo> getGenerics();

  //TODO: Remove once the type loader exists MULE-14040
  java.lang.reflect.Type getReflectType();

  String getTypeName();
}
