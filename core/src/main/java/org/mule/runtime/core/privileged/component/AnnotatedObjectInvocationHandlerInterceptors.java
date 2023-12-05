/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import static java.lang.Integer.toHexString;
import static java.lang.reflect.Modifier.isStatic;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

public class AnnotatedObjectInvocationHandlerInterceptors {

  public static <T, A> T removeDynamicAnnotations(A annotated) {
    if (annotated instanceof DynamicallyComponent) {
      Class<?> baseClass = annotated.getClass().getSuperclass();

      Map<String, Field> fieldsByName = new HashMap<>();
      Class<?> currentClass = baseClass;
      while (currentClass != Object.class) {
        Field[] targetFields = currentClass.getDeclaredFields();

        for (Field field : targetFields) {
          if (!isStatic(field.getModifiers()) && !fieldsByName.containsKey(field.getName())) {
            fieldsByName.put(field.getName(), field);
          }
        }

        currentClass = currentClass.getSuperclass();
      }

      try {
        T base = (T) baseClass.newInstance();
        for (Field field : fieldsByName.values()) {
          boolean acc = field.isAccessible();
          field.setAccessible(true);
          try {
            field.set(base, field.get(annotated));
          } finally {
            field.setAccessible(acc);
          }
        }

        return base;
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    } else {
      return (T) annotated;
    }
  }

  public static class ComponentInterceptor extends AbstractComponent {

    @Override
    public Object getAnnotation(QName qName) {
      return super.getAnnotation(qName);
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return super.getAnnotations();
    }

    @Override
    public void setAnnotations(Map<QName, Object> newAnnotations) {
      super.setAnnotations(newAnnotations);
    }

    @Override
    public ComponentLocation getLocation() {
      return super.getLocation();
    }

    @Override
    public Location getRootContainerLocation() {
      return super.getRootContainerLocation();
    }

    @Override
    public ComponentIdentifier getIdentifier() {
      return super.getIdentifier();
    }

    @Override
    public String getRepresentation() {
      return super.getRepresentation();
    }

    @Override
    public String getDslSource() {
      return super.getDslSource();
    }
  }

  public static class ComponentAdditionalInterceptor {

    private Component obj;

    public Object writeReplace()
        throws Throwable {
      return removeDynamicAnnotations(obj);
    }

    @Override
    public String toString() {
      String base = obj.getClass().getName() + "@" + toHexString(obj.hashCode()) + "; location: ";
      if (obj.getLocation() != null) {
        return base + obj.getLocation().getLocation();
      } else {
        return base + "(null)";
      }
    }

    public void setObj(Component obj) {
      this.obj = obj;
    }
  }

}
