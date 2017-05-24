/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.report;

import static org.mule.tck.report.HeapDumper.dumpHeap;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Rule that generates a heap dump on failure.
 * <p>
 * Useful for troubleshooting failures in memory leak prevention tests.
 */
public class HeapDumpOnFailure extends TestWatcher {

  @Override
  protected void failed(Throwable e, Description description) {
    super.failed(e, description);
    dumpHeap(description.getTestClass().getName() + description.getMethodName() + ".hprof", true);
  }
}
