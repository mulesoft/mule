/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
