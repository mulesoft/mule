/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An object that owns message processors and delegates startup/shutdown events to them.
 */
public abstract class AbstractMessageProcessorOwner extends AbstractMuleObjectOwner<Processor>
    implements Lifecycle, MuleContextAware, FlowConstructAware, AnnotatedObject, MessageProcessorContainer {

  private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

  public final Object getAnnotation(QName name) {
    return annotations.get(name);
  }

  public final Map<QName, Object> getAnnotations() {
    return Collections.unmodifiableMap(annotations);
  }

  public synchronized final void setAnnotations(Map<QName, Object> newAnnotations) {
    annotations.clear();
    annotations.putAll(newAnnotations);
  }

  protected List<Processor> getOwnedObjects() {
    return getOwnedMessageProcessors();
  }

  protected abstract List<Processor> getOwnedMessageProcessors();

}

