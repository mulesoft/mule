/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

import static org.mule.tck.report.ThreadDumper.logThreadDump;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import org.slf4j.Logger;

public class PollingProber implements Prober {

  private static final Logger LOGGER = getLogger(PollingProber.class);

  public static final long DEFAULT_TIMEOUT = 1000;
  public static final long DEFAULT_POLLING_INTERVAL = 100;

  private final long timeoutMillis;
  private final long pollDelayMillis;

  public static void check(long timeoutMillis, long pollDelayMillis, CheckedSupplier<Boolean> probe) {
    new PollingProber(timeoutMillis, pollDelayMillis).check(new JUnitLambdaProbe(probe));
  }

  /**
   * Similar to {@link #check(long, long, CheckedSupplier)} only that this one is expecting for the probe condition
   * to <b>NEVER</b> be met. If the condition is ever met, then an {@link AssertionError} is thrown
   */
  public static void checkNot(long timeoutMillis, long pollDelayMillis, CheckedSupplier<Boolean> probe) {
    try {
      check(timeoutMillis, pollDelayMillis, probe);
    } catch (AssertionError e) {
      return;
    }

    throw new AssertionError("Was expecting probe to fail");
  }

  public PollingProber() {
    this(DEFAULT_TIMEOUT, DEFAULT_POLLING_INTERVAL);
  }

  public PollingProber(long timeoutMillis, long pollDelayMillis) {
    this.timeoutMillis = timeoutMillis;
    this.pollDelayMillis = pollDelayMillis;
  }

  public static void probe(CheckedSupplier<Boolean> probable) {
    probe(probable, () -> null);
  }

  public static void probe(CheckedSupplier<Boolean> probable, CheckedSupplier<String> failureDescription) {
    new PollingProber().check(new JUnitLambdaProbe(probable, failureDescription));
  }

  public static void probe(CheckedSupplier<Boolean> probable, CheckedFunction<AssertionError, String> failureDescription) {
    new PollingProber().check(new JUnitLambdaProbe(probable, failureDescription));
  }

  public static void probe(long timeoutMillis, long pollDelayMillis, CheckedSupplier<Boolean> probable) {
    probe(timeoutMillis, pollDelayMillis, probable, () -> null);
  }

  public static void probe(long timeoutMillis, long pollDelayMillis, CheckedSupplier<Boolean> probable,
                           CheckedSupplier<String> failureDescription) {
    new PollingProber(timeoutMillis, pollDelayMillis).check(new JUnitLambdaProbe(probable, failureDescription));
  }

  public static void probe(long timeoutMillis, long pollDelayMillis, CheckedSupplier<Boolean> probable,
                           CheckedFunction<AssertionError, String> failureDescription) {
    new PollingProber(timeoutMillis, pollDelayMillis).check(new JUnitLambdaProbe(probable, failureDescription));
  }

  @Override
  public void check(Probe probe) {
    if (!poll(probe)) {
      LOGGER.error("test timed out. Maybe due to a deadlock?");
      logThreadDump();

      throw new AssertionError(probe.describeFailure());
    }
  }

  private boolean poll(Probe probe) {
    Timeout timeout = new Timeout(timeoutMillis);

    while (true) {
      LOGGER.error("Probe poll: " + probe.describeFailure());
      if (probe.isSatisfied()) {
        LOGGER.error("Probe poll: Is Satisfied");
        return true;
      } else if (timeout.hasTimedOut()) {
        LOGGER.error("Probe poll: hasTimedOut()");
        return false;
      } else {
        LOGGER.error("Probe poll: Waiting " + pollDelayMillis);
        waitFor(pollDelayMillis);
      }
    }
  }

  private void waitFor(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      throw new IllegalStateException("unexpected interrupt", e);
    }
  }
}
