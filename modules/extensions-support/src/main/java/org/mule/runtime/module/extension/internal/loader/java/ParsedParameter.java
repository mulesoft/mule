/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import org.mule.metadata.api.model.MetadataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Intermediate representation of a parameter used to decouple an interchangeable introspection mechanism from the extension's API
 * model
 *
 * @since 3.7.0
 */
final class ParsedParameter implements AnnotatedElement {

  private String name;
  private MetadataType type;
  private boolean required;
  private Object defaultValue;
  private Class<?> typeRestriction = null;
  private boolean advertised = true;
  private Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
  private Optional<Parameter> implementingParameter = Optional.empty();

  ParsedParameter(Map<Class<? extends Annotation>, Annotation> annotations) {
    this.annotations = annotations;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  MetadataType getType() {
    return type;
  }

  void setType(MetadataType type) {
    this.type = type;
  }

  boolean isRequired() {
    return required;
  }

  void setRequired(boolean required) {
    this.required = required;
  }

  Object getDefaultValue() {
    return defaultValue;
  }

  void setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
  }

  Class<?> getTypeRestriction() {
    return typeRestriction;
  }

  void setTypeRestriction(Class<?> typeRestriction) {
    this.typeRestriction = typeRestriction;
  }

  public boolean isAdvertised() {
    return advertised;
  }

  public void setAdvertised(boolean advertised) {
    this.advertised = advertised;
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
    return (T) annotations.get(annotationType);
  }

  public void setImplementingParameter(Parameter implementingParameter) {
    this.implementingParameter = Optional.of(implementingParameter);
  }

  public Optional<Parameter> getImplementingParameter() {
    return this.implementingParameter;
  }

  @Override
  public Annotation[] getAnnotations() {
    return (Annotation[]) annotations.values().toArray();
  }

  @Override
  public Annotation[] getDeclaredAnnotations() {
    return getAnnotations();
  }
}
