/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories.streaming;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

import javax.inject.Inject;

public abstract class AbstractCursorProviderObjectFactory<T> extends AbstractAnnotatedObjectFactory<T> {

  @Inject
  protected StreamingManager streamingManager;

  @Override
  public T getObject() throws Exception {
    initialiseIfNeeded(streamingManager);
    return super.getObject();
  }
}
