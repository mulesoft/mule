/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.versionNotSet;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.DefaultMuleException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Threads;

public class MessagingExceptionBenchmark extends AbstractBenchmark {

  @Benchmark
  @Threads(1)
  public MuleException stringSingleThread() {
    return new DefaultMuleException("customMessage");
  }

  @Benchmark
  @Threads(1)
  public MuleException messageSingleThead() {
    return new DefaultMuleException(versionNotSet());
  }

  @Benchmark
  @Threads(4)
  public MuleException messageMultiThread() {
    return new DefaultMuleException(versionNotSet());
  }

  @Benchmark
  @Threads(4)
  public MuleException stringMultiThread() {
    return new DefaultMuleException("customMessage");
  }

}
