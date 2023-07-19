/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.factories.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import javax.inject.Inject;

@NoExtend
public abstract class AbstractCursorProviderObjectFactory<T> extends AbstractComponentFactory<T> {

  @Inject
  protected StreamingManager streamingManager;

  @Override
  public T getObject() throws Exception {
    initialiseIfNeeded(streamingManager);
    return doGetObject();
  }
}
