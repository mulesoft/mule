/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@link CursorProviderFactory} which never generates a {@link CursorProviderFactory}
 *
 * @since 4.4.0
 */
public class NullCursorProviderFactory implements CursorProviderFactory {

  @Override
  public Object of(EventContext eventContext, Object value, ComponentLocation originatingLocation) {
    return value;
  }

  @Override
  public Object of(EventContext eventContext, Object value) {
    return value;
  }

  @Override
  public Object of(CoreEvent event, Object value, ComponentLocation originatingLocation) {
    return value;
  }

  @Override
  public Object of(CoreEvent event, Object value) {
    return value;
  }

  @Override
  public boolean accepts(Object value) {
    return false;
  }
}
