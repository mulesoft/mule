/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.internal.factories;

import org.mule.runtime.api.component.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Extends spring's {@link AbstractFactoryBean} by adding annotations. This will then be passed to the built bean.
 */
public abstract class ComponentFactoryBean<B extends Component> extends AbstractFactoryBean<B>
    implements Component {

  /*
   * From AbstractAnnotatedObject:
   */

  private final Map<QName, Object> annotations = new ConcurrentHashMap<>();

  @Override
  public final Object getAnnotation(QName qName) {
    return annotations.get(qName);
  }

  @Override
  public final Map<QName, Object> getAnnotations() {
    return Collections.unmodifiableMap(annotations);
  }

  @Override
  public synchronized final void setAnnotations(Map<QName, Object> newAnnotations) {
    annotations.clear();
    annotations.putAll(newAnnotations);
  }

  @Override
  public B createInstance() throws Exception {
    B annotatedInstance = doCreateInstance();
    annotatedInstance.setAnnotations(getAnnotations());
    return annotatedInstance;
  }

  protected abstract B doCreateInstance() throws Exception;

}
