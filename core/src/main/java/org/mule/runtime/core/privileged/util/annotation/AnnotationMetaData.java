/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Member;

/**
 * A data class that associates context information about an annotation. This class allows for associated annotation data to be
 * passed between methods.
 */
public class AnnotationMetaData {

  private ElementType type;

  private Member member;

  private Class clazz;

  private Annotation annotation;

  public AnnotationMetaData(Class clazz, Member member, ElementType type, Annotation annotation) {
    this.type = type;
    this.clazz = clazz;
    this.member = member;
    this.annotation = annotation;
  }

  public ElementType getType() {
    return type;
  }

  public String getElementName() {
    if (member == null) {
      return clazz.getName();
    }
    return member.getName();
  }

  public Annotation getAnnotation() {
    return annotation;
  }

  public Member getMember() {
    return member;
  }

  public Class getClazz() {
    return clazz;
  }

  @Override
  public String toString() {
    return "AnnotationMetaData{" + "type=" + type + ", member=" + member + ", clazz=" + clazz + ", annotation=" + annotation
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AnnotationMetaData that = (AnnotationMetaData) o;

    if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null) {
      return false;
    }
    if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) {
      return false;
    }
    if (member != null ? !member.equals(that.member) : that.member != null) {
      return false;
    }
    if (type != that.type) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (member != null ? member.hashCode() : 0);
    result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
    result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
    return result;
  }
}
