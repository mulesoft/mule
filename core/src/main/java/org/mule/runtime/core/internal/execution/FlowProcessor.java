/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.Map;

import javax.xml.namespace.QName;

import org.reactivestreams.Publisher;

/**
 * Wraps a {@link ReactiveProcessor} that represents the execution of a flow with the metadata from the flow itself.
 *
 * @since 4.4, extracted from {@link FlowProcessMediator}.
 */
public class FlowProcessor implements ReactiveProcessor, Component {

  private final ReactiveProcessor processor;
  private final FlowConstruct flowConstruct;

  public FlowProcessor(ReactiveProcessor processor, FlowConstruct flowConstruct) {
    this.processor = processor;
    this.flowConstruct = flowConstruct;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return processor.apply(publisher);
  }

  @Override
  public Object getAnnotation(QName name) {
    return flowConstruct.getAnnotation(name);
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return flowConstruct.getAnnotations();
  }

  @Override
  public void setAnnotations(Map<QName, Object> annotations) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentLocation getLocation() {
    return flowConstruct.getLocation();
  }

  @Override
  public Location getRootContainerLocation() {
    return flowConstruct.getRootContainerLocation();
  }
}
