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
import java.util.Optional;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

public class SubscribedProcessors {

  private boolean trackSubscribedComponents = true;
  private List<String> subscribedComponents = Collections.emptyList();
  private int subscribedProcessorsCount = 0;

  private SubscribedProcessors(boolean trackSubscribedComponents) {
    this.trackSubscribedComponents = trackSubscribedComponents;
  }

  private SubscribedProcessors(int subscribedProcessorsCount) {
    this.subscribedProcessorsCount = subscribedProcessorsCount;
  }

  private SubscribedProcessors(int subscribedProcessorsCount, List<String> subscribedComponents) {
    this.subscribedProcessorsCount = subscribedProcessorsCount;
    this.subscribedComponents = subscribedComponents;
  }

  // We intentionally leave the previous SubscribedProcessor instance unchanged because the context is sent
  // through multiple subscription paths that must track independent collections of subscribed components.
  // **************************/--- A
  // Example: ---C--(merge)----
  // **************************\--- B
  // Where A and B onSubscribe must see only C as a subscribed component.
  private SubscribedProcessors addSubscribedProcessor(Processor processor) {
    SubscribedProcessors updatedSubscribedProcessors;
    if (trackSubscribedComponents && getProcessorComponentLocation(processor) != null) {
      List<String> updatedSubscribedComponents = new ArrayList<>(subscribedComponents.size() + 1);
      updatedSubscribedComponents.addAll(subscribedComponents);
      updatedSubscribedComponents.add(getProcessorComponentLocation(processor));
      updatedSubscribedProcessors = new SubscribedProcessors(subscribedProcessorsCount + 1, updatedSubscribedComponents);
    } else {
      updatedSubscribedProcessors = new SubscribedProcessors(subscribedProcessorsCount + 1);
    }
    return updatedSubscribedProcessors;
  }

  public int getSubscribedProcessorsCount() {
    return subscribedProcessorsCount;
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
        + "Subscribed processors: " + subscribedProcessorsCount;
  }

  public static class SubscribedProcessorsContext {

    private final Context context;
    private final SubscribedProcessors subscribedProcessors;

    private SubscribedProcessorsContext(Context context) {
      this.context = context;
      this.subscribedProcessors = context.getOrDefault("SUBSCRIBED_PROCESSORS", new SubscribedProcessors(false));
    }

    private SubscribedProcessorsContext(Context context, boolean trackSubscribedComponents) {
      this.context = context;
      this.subscribedProcessors =
          context.getOrDefault("SUBSCRIBED_PROCESSORS", new SubscribedProcessors(trackSubscribedComponents));
    }

    public static SubscribedProcessorsContext subscribedProcessors(Context context) {
      return new SubscribedProcessorsContext(context);
    }

    public static SubscribedProcessorsContext subscribedProcessors(Context context, boolean trackSubscribedComponents) {
      return new SubscribedProcessorsContext(context, trackSubscribedComponents);
    }

    public static Optional<SubscribedProcessors> subscribedProcessors(ContextView context) {
      return context.getOrEmpty("SUBSCRIBED_PROCESSORS");
    }

    public Context addSubscribedProcessor(Processor processor) {
      subscribedProcessors.addSubscribedProcessor(processor);
      return context.put("SUBSCRIBED_PROCESSORS", subscribedProcessors.addSubscribedProcessor(processor));
    }
  }
}
