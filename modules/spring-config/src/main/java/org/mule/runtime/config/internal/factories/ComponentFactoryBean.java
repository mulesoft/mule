/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
