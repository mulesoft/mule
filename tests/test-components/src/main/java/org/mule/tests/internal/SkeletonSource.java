/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.InputStream;

/**
 * Mock message source that provides access to the Processor set by the owner Flow.
 */
@MediaType(value = ANY, strict = false)
public class SkeletonSource extends Source<InputStream, InputStream> {

  private volatile boolean started = false;

  @Override
  public synchronized void onStart(SourceCallback sourceCallback) throws MuleException {
    started = true;
  }

  public synchronized boolean isStarted() {
    return started;
  }

  @Override
  public synchronized void onStop() {
    started = false;
  }
}
