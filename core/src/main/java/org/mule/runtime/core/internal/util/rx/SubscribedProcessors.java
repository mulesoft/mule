/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import reactor.util.context.Context;

public class SubscribedProcessors {

  private Context context;
  private boolean trackSubscribedComponents = false;
  private List<String> subscribedComponents = Collections.emptyList();
  private int subscribedProcessors = 0;

  private SubscribedProcessors(Context context, boolean trackSubscribedComponents) {
    this.context = context;
    this.trackSubscribedComponents = trackSubscribedComponents;
  }

  private SubscribedProcessors(int subscribedProcessors) {
    this.subscribedProcessors = subscribedProcessors;
  }

  private SubscribedProcessors(int subscribedProcessors, List<String> subscribedComponents) {
    this.subscribedProcessors = subscribedProcessors;
    this.subscribedComponents = subscribedComponents;
  }

  public static SubscribedProcessors subscribedProcessors(Context context) {
    return context.getOrDefault("SUBSCRIBED_PROCESSORS", new SubscribedProcessors(context, false));
  }

  public static SubscribedProcessors subscribedProcessors(Context context, boolean trackSubscribedComponents) {
    return context.getOrDefault("SUBSCRIBED_PROCESSORS", new SubscribedProcessors(context, trackSubscribedComponents));
  }

  // We intentionally leave the previous SubscribedProcessor instance unchanged because the context is sent
  // through multiple subscription paths that must track independent collections of subscribed components.
  // **************************/--- A
  // Example: ---C--(merge)----
  // **************************\--- B
  // Where A and B onSubscribe must see only C as a subscribed component.
  public Context addSubscribedProcessor(Processor processor) {
    SubscribedProcessors updatedSubscribedProcessors;
    if (trackSubscribedComponents && getProcessorComponentLocation(processor) != null) {
      List<String> updatedSubscribedComponents = new ArrayList<>(subscribedComponents.size() + 1);
      updatedSubscribedComponents.addAll(subscribedComponents);
      updatedSubscribedComponents.add(getProcessorComponentLocation(processor));
      updatedSubscribedProcessors = new SubscribedProcessors(subscribedProcessors + 1, updatedSubscribedComponents);
    } else {
      updatedSubscribedProcessors = new SubscribedProcessors(subscribedProcessors + 1);
    }
    Context updatedContext = context.put("SUBSCRIBED_PROCESSORS", updatedSubscribedProcessors);
    updatedSubscribedProcessors.context = updatedContext;
    return updatedContext;
  }

  private static String getProcessorComponentLocation(Processor processor) {
    if (processor instanceof Component && ((Component) processor).getLocation() != null) {
      return ((Component) processor).getLocation().getLocation();
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return Thread.currentThread().getName() + " - StackTrace lines: " + Thread.currentThread().getStackTrace().length + " - "
        + "Subscribed processors: " + subscribedProcessors;
  }
}
