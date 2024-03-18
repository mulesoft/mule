/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.probe;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.mule.tck.probe.PollingProber.DEFAULT_TIMEOUT;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class PollingProberTestCase extends AbstractMuleTestCase {

  private PollingProber prober;

  @Before
  public void setup() {
    // Create a prober with a fast polling
    prober = new PollingProber(DEFAULT_TIMEOUT, 1);
  }

  @Test
  public void probeBecomesTrueAfterFiveTries() {
    prober.check(new ProbeWhichBecomesTrueAfterNTries(5, false));
  }

  @Test
  public void probeAssertsTrueAfterFiveTries() {
    prober.check(new ProbeWhichBecomesTrueAfterNTries(5, true));
  }

  @Test
  public void jUnitProbeBecomesTrueAfterFiveTries() {
    prober.check(new JUnitAdapter(new ProbeWhichBecomesTrueAfterNTries(5, false)));
  }


  @Test
  public void jUnitProbeAssertsTrueAfterFiveTries() {
    prober.check(new JUnitAdapter(new ProbeWhichBecomesTrueAfterNTries(5, true)));
  }

  private static class ProbeWhichBecomesTrueAfterNTries implements Probe {

    private int currentTries = 0;
    private int failingTries;
    private boolean isAssertion;

    private ProbeWhichBecomesTrueAfterNTries(int failingTries, boolean isAssertion) {
      this.failingTries = failingTries;
      this.isAssertion = isAssertion;
    }

    @Override
    public boolean isSatisfied() {
      currentTries += 1;

      if (isAssertion) {
        assertThat(currentTries, greaterThan(failingTries));
        return true;
      } else {
        return currentTries >= failingTries;
      }
    }

    @Override
    public String describeFailure() {
      return format("You need to call the probe %d times before it becomes true", failingTries);
    }
  }

  private static class JUnitAdapter extends JUnitProbe {

    private final ProbeWhichBecomesTrueAfterNTries delegate;

    private JUnitAdapter(ProbeWhichBecomesTrueAfterNTries delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean test() {
      return delegate.isSatisfied();
    }

    @Override
    public String describeFailure() {
      return delegate.describeFailure();
    }
  }
}
