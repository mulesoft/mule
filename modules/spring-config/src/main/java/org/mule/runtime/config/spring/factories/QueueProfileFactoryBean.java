/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.store.QueueStore;
import org.mule.runtime.core.config.QueueProfile;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.config.AbstractFactoryBean;

public class QueueProfileFactoryBean extends AbstractFactoryBean<QueueProfile>
    implements MuleContextAware, ObjectFactory<QueueProfile> {

  private final Map<QName, Object> annotations = new ConcurrentHashMap<>();
  private int maxOutstandingMessages;
  private MuleContext muleContext;
  private QueueStore<Serializable> queueStore;

  @Override
  public Object getAnnotation(QName name) {
    return annotations.get(name);
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return unmodifiableMap(annotations);
  }

  @Override
  public void setAnnotations(Map<QName, Object> annotations) {
    this.annotations.putAll(annotations);
  }

  @Override
  public ComponentLocation getLocation() {
    return (ComponentLocation) getAnnotation(LOCATION_KEY);
  }

  @Override
  public Class<?> getObjectType() {
    return QueueProfile.class;
  }

  @Override
  protected QueueProfile createInstance() throws Exception {
    QueueStore<Serializable> objectStore = queueStore;
    if (objectStore == null) {
      objectStore = muleContext.getRegistry().lookupObject(MuleProperties.QUEUE_STORE_DEFAULT_IN_MEMORY_NAME);
    }

    return new QueueProfile(getMaxOutstandingMessages(), objectStore);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    muleContext = context;
  }

  public int getMaxOutstandingMessages() {
    return maxOutstandingMessages;
  }

  public void setMaxOutstandingMessages(int maxOutstandingMessages) {
    this.maxOutstandingMessages = maxOutstandingMessages;
  }

  public void setQueueStore(QueueStore<Serializable> queueStore) {
    this.queueStore = queueStore;
  }

  public QueueStore<Serializable> getQueueStore() {
    return queueStore;
  }
}


