/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Test source that provides access to the {@link Processor} set by the owner {@link org.mule.runtime.core.api.construct.Flow}.
 *
 * @since 4.0
 */
public class SkeletonSource extends AbstractComponent implements MessageSource, Startable {

  private Processor listener;
  private volatile boolean started = false;

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

  public Processor getListener() {
    return listener;
  }

  @Override
  public synchronized void start() throws MuleException {
    started = true;
  }

  public synchronized boolean isStarted() {
    return started;
  }
}
