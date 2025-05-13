/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import org.mule.runtime.core.api.processor.Processor;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Optional;

public class SubscribedProcessorsContext {

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
    return context.put("SUBSCRIBED_PROCESSORS", subscribedProcessors.addSubscribedProcessor(processor));
  }

  public int getSubscribedProcessorsCount() {
    return subscribedProcessors.getSubscribedProcessorsCount();
  }
}
