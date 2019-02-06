/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import javax.lang.model.element.TypeElement;

/**
 * A generic contract for any kind of component that is based on a Class
 *
 * @since 4.0
 */
@NoImplement
public interface Type extends WithAnnotations, WithName, WithAlias, WithDeclaringClass, WithElement {

  /**
   * @return A list of {@link FieldElement} that represent the list of {@link Field} that the {@link Type} declares
   */
  List<FieldElement> getFields();

  /**
   * @return A list of {@link FieldElement} that represent the list of {@link Field} propertyes that the {@link Type} declares
   */
  List<PropertyElement> getProperties();

  /**
   * @param annotations classes that the fields of this type should be annotated with
   * @return A list of {@link FieldElement} that represent the list of {@link Field} that the {@link Type} declares and are
   * annotated with the given annotation
   */
  List<FieldElement> getAnnotatedFields(Class<? extends Annotation>... annotations);

  /**
   * Checks the assignability of the current type from the given {@link Class}
   *
   * @param clazz The class to check
   * @return a boolean indicating whether the type is assignable or not from the given class
   * @since 4.1
   */
  boolean isAssignableFrom(Class<?> clazz);

  /**
   * Checks the assignability of the current type from the given {@link Type}
   *
   * @param type The type to check
   * @return a boolean indicating whether the type is assignable or not to the given {@link Type}
   * @since 4.1
   */
  boolean isAssignableFrom(Type type);

  /**
   * Checks the assignability of the current type to the given {@link Class}
   *
   * @param clazz The class to check
   * @return a boolean indicating whether the type is assignable or not to the given class
   * @since 4.1
   */
  boolean isAssignableTo(Class<?> clazz);

  /**
   * Checks the assignability of the current type to the given {@link Type}
   *
   * @param type The type to check
   * @return a boolean indicating whether the type is assignable or not to the given {@link Type}
   * @since 4.1
   */
  boolean isAssignableTo(Type type);

  /**
   * Checks equality of the current type to the given {@link Type}
   *
   * @param type The type to check equality
   * @return a boolean indicating whether the type is the same type or not
   * @since 4.1
   */
  boolean isSameType(Type type);

  /**
   * Checks equality of the current type to the given {@link Class}
   *
   * @param clazz The type to check equality
   * @return a boolean indicating whether the type is the same type or not
   * @since 4.1
   */
  boolean isSameType(Class<?> clazz);

  /**
   * @return a boolean indicating if the current type is instantiable or not.
   * @since 4.1
   */
  boolean isInstantiable();

  /**
   * @return The generics for the current type.
   * @since 4.1
   */
  List<TypeGeneric> getGenerics();

  /**
   * @param superType The {@link Class} with generics, this can be a Interface or a normal Class
   * @return The list of generics types from the given super type class.
   * @since 4.1.2
   */
  List<Type> getSuperTypeGenerics(Class superType);

  /**
   * @param interfaceType The {@link Class} with generics
   * @return The list of generics types from the given interface class.
   * @since 4.1
   */
  @Deprecated
  default List<Type> getInterfaceGenerics(Class interfaceType) {
    return getSuperTypeGenerics(interfaceType);
  }

  /**
   * @return The current type described as a {@link MetadataType}
   * @since 4.1
   */
  MetadataType asMetadataType();

  String getTypeName();

  /**
   * @return The {@link ClassInformationAnnotation} describing the current {@link Type}
   * @since 4.1
   */
  ClassInformationAnnotation getClassInformation();

  /**
   * @return A boolean indicating if this type may be considered as an {@link AnyType}
   * @since 4.1
   */
  boolean isAnyType();

  /**
   * {@inheritDoc}
   */
  @Override
  Optional<TypeElement> getElement();

  /**
   * @param name
   * @param parameterTypes
   * @return The {@link MethodElement} if present
   * @since 4.2
   */
  Optional<MethodElement> getMethod(String name, Class<?>... parameterTypes);
}
