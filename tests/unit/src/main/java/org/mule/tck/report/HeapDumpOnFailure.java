/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
