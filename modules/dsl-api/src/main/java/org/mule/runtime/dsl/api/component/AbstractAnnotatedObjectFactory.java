/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.dsl.api.component.config.ComponentIdentifier.ANNOTATION_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ComponentLocation;
import org.mule.runtime.api.meta.AnnotatedObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

/**
 * Basic implementation of {@link AnnotatedObjectFactory} that handles all annotation related behavior
 * including {@link ObjectFactory#getObject()}.
 *
 * @param <T> the type of the object to be created, which should be an {@link AnnotatedObject}.
 */
public abstract class AbstractAnnotatedObjectFactory<T> implements AnnotatedObjectFactory<T> {

  private final Map<QName, Object> annotations = new ConcurrentHashMap<>();

  @Override
  public final Object getAnnotation(QName qName) {
    return annotations.get(qName);
  }

  @Override
  public final Map<QName, Object> getAnnotations() {
    return unmodifiableMap(annotations);
  }

  @Override
  public ComponentIdentifier getIdentifier() {
    return new ComponentIdentifier() {

      @Override
      public String getNamespace() {
        return ((org.mule.runtime.dsl.api.component.config.ComponentIdentifier) getAnnotation(ANNOTATION_NAME)).getNamespace();
      }

      @Override
      public String getName() {
        return ((org.mule.runtime.dsl.api.component.config.ComponentIdentifier) getAnnotation(ANNOTATION_NAME)).getName();
      }
    };
  }

  @Override
  public ComponentLocation getLocation(String flowPath) {
    if (flowPath == null) {
      return null;
    } else {
      return new ComponentLocation() {

        @Override
        public String getPath() {
          return flowPath;
        }

        @Override
        public String getFileName() {
          return (String) getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName"));
        }

        @Override
        public int getLineInFile() {
          return (int) getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine"));
        }
      };
    }
  }

  @Override
  public synchronized final void setAnnotations(Map<QName, Object> newAnnotations) {
    annotations.clear();
    annotations.putAll(newAnnotations);
  }

  @Override
  public T getObject() throws Exception {
    T annotatedInstance = doGetObject();
    //TODO - MULE-10971: Remove if block once all extension related objects are AnnotatedObjects
    if (annotatedInstance instanceof AnnotatedObject) {
      ((AnnotatedObject) annotatedInstance).setAnnotations(getAnnotations());
    }
    return annotatedInstance;
  }
}
