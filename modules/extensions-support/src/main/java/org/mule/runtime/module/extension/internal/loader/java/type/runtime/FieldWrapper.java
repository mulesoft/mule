/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static org.mule.runtime.module.extension.internal.loader.java.contributor.InfrastructureTypeResolver.getInfrastructureType;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import static org.springframework.core.ResolvableType.forField;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.loader.util.InfrastructureTypeUtils.MetadataTypeBasedInfrastructureType;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.lang.model.element.VariableElement;

/**
 * Wrapper for {@link Field} that provide utility methods to facilitate the introspection of a {@link Field}
 *
 * @since 4.0
 */
public class FieldWrapper implements FieldElement {

  private final Field field;
  private final ClassTypeLoader typeLoader;
  private FieldSetter fieldSetter;

  public FieldWrapper(Field field, ClassTypeLoader typeLoader) {
    this.field = field;
    this.typeLoader = typeLoader;
  }

  /**
   * @return the wrapped {@link Field}
   */
  @Override
  public Optional<Field> getField() {
    return ofNullable(field);
  }

  @Override
  public void set(Object object, Object value) {
    if (fieldSetter == null) {
      fieldSetter = new FieldSetter<>(field);
    }
    fieldSetter.set(object, value);
  }

  @Override
  public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
    return field.getAnnotation(annotation) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return field.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(field.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TypeWrapper getType() {
    return new TypeWrapper(forField(field), typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAlias() {
    return getInfrastructureType(getType())
        .map(MetadataTypeBasedInfrastructureType::getName)
        .orElse(FieldElement.super.getAlias());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOwnerDescription() {
    return format("Class: '%s'", field.getDeclaringClass().getSimpleName());
  }

  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    return isAnnotatedWith(annotationClass)
        ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, field, typeLoader))
        : empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Stream<Type> getAnnotations() {
    return stream(field.getAnnotations()).map(ann -> new TypeWrapper(ann.annotationType(), typeLoader));
  }

  @Override
  public Optional<VariableElement> getElement() {
    return empty();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FieldWrapper) {
      return Objects.equals(((FieldWrapper) obj).field, field);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }
}
