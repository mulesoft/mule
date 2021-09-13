/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Optional.ofNullable;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class ForeachInternalContextManager {

  private static Optional<ForeachInternalContext> from(CoreEvent event) {
    return ofNullable((ForeachInternalContext) ((InternalEvent) event).<ForeachInternalContext>getForeachInternalContext());
  }

  public static ForeachContext getContext(CoreEvent event) {
    return from(event).map(ctx -> ctx.get(event)).orElse(null);
  }

  public static void addContext(CoreEvent event, ForeachContext context) {
    ForeachInternalContext foreachInternalContext = from(event).orElseGet(() -> {
      ForeachInternalContext ctx = new ForeachInternalContext();
      ((InternalEvent) event).setForeachInternalContext(ctx);
      return ctx;
    });
    foreachInternalContext.put(event, context);
  }

  public static void removeContext(CoreEvent event) {
    from(event).map(ctx -> ctx.remove(event));
  }

  static class ForeachInternalContext implements EventInternalContext<ForeachInternalContext> {

    private Map<String, ForeachContext> contexts = new HashMap<>();

    public ForeachContext get(CoreEvent event) {
      return contexts.get(event.getContext().getId());
    }

    public void put(CoreEvent event, ForeachContext context) {
      contexts.put(event.getContext().getId(), context);
    }

    public ForeachContext remove(CoreEvent event) {
      return contexts.remove(event.getContext().getId());
    }

    @Override
    public ForeachInternalContext copy() {
      ForeachInternalContext other = new ForeachInternalContext();
      other.contexts.putAll(this.contexts);
      return other;
    }
  }
}
