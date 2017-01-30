/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.ComponentType.INTERCEPTING;
import static org.mule.runtime.api.component.ComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.api.component.ComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.ComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.dsl.api.component.config.ComponentIdentifier.ANNOTATION_NAME;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ComponentLocation;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageRouter;
import org.mule.runtime.core.api.routing.OutboundRouter;
import org.mule.runtime.core.api.routing.SelectiveRouter;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Base implementation for {@link AnnotatedObject}
 */
public abstract class AbstractAnnotatedObject implements AnnotatedObject {

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
  public synchronized void setAnnotations(Map<QName, Object> newAnnotations) {
    annotations.clear();
    annotations.putAll(newAnnotations);
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

      @Override
      public ComponentType getComponentType() {
        // TODO improve this implementation
        if (AbstractAnnotatedObject.this instanceof MessageSource) {
          return SOURCE;
        } else if (AbstractAnnotatedObject.this instanceof OutboundRouter
            || AbstractAnnotatedObject.this instanceof SelectiveRouter || AbstractAnnotatedObject.this instanceof MessageRouter) {
          return ROUTER;
        } else if (AbstractAnnotatedObject.this instanceof InterceptingMessageProcessor) {
          return INTERCEPTING;
        } else {
          return PROCESSOR;
        }
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
        public Optional<String> getFileName() {
          return of((String) getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName")));
        }

        @Override
        public Optional<Integer> getLineInFile() {
          return of((int) getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine")));
        }
      };
    }
  }
}
