/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.internal.message.InternalEvent;

import java.util.Map;

import javax.xml.namespace.QName;

public final class VoidEventFactory {

  private static final String INITIALIZER_EVENT = "InitializerEvent";

  private VoidEventFactory() {}

  /**
   * Creates an empty event for extension initialization purposes
   *
   * @return a new {@link CoreEvent}
   */
  public static CoreEvent getVoidEvent() {
    return getVoidEvent(null);
  }

  /**
   * Creates an empty event for extension initialization purposes
   *
   * @param muleContext context on which the event will be associated.
   * @return a new {@link CoreEvent}
   */
  public static CoreEvent getVoidEvent(MuleContext muleContext) {
    FlowConstruct flowConstruct = new FlowConstruct() {

      @Override
      public Object getAnnotation(QName name) {
        return null;
      }

      @Override
      public Map<QName, Object> getAnnotations() {
        return emptyMap();
      }

      @Override
      public void setAnnotations(Map<QName, Object> annotations) {}

      @Override
      public ComponentLocation getLocation() {
        return null;
      }

      @Override
      public Location getRootContainerLocation() {
        return null;
      }
      // TODO MULE-9076: This is only needed because the muleContext is get from the given flow.

      @Override
      public MuleContext getMuleContext() {
        return muleContext;
      }

      @Override
      public String getServerId() {
        return "InitialiserServer";
      }

      @Override
      public String getUniqueIdString() {
        return getUUID();
      }

      @Override
      public String getName() {
        return "InitialiserEventFlow";
      }

      @Override
      public LifecycleState getLifecycleState() {
        return null;
      }

      @Override
      public FlowExceptionHandler getExceptionListener() {
        return null;
      }

      @Override
      public DefaultFlowConstructStatistics getStatistics() {
        return null;
      }
    };
    return InternalEvent.builder(create(flowConstruct, fromSingleComponent(INITIALIZER_EVENT)))
        .message(of(null))
        .build();
  }
}
