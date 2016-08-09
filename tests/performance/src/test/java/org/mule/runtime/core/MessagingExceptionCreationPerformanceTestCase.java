/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessagingExceptionCreationPerformanceTestCase extends AbstractMuleTestCase {

  @Rule
  public ContiPerfRule rule = new ContiPerfRule();

  @Mock
  private MuleContext muleContext;

  @Override
  public int getTestTimeoutSecs() {
    return 120;
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void stringSingleThread() {
    for (int i = 0; i < 1000; i++) {
      new DefaultMuleException("customMessage");
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
  public void messageSingleThead() {
    for (int i = 0; i < 1000; i++) {
      new DefaultMuleException(CoreMessages.agentsRunning());
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 4, warmUp = 5000)
  public void messageMultiThread() {
    for (int i = 0; i < 1000; i++) {
      new DefaultMuleException(CoreMessages.agentsRunning());
    }
  }

  @Test
  @PerfTest(duration = 15000, threads = 4, warmUp = 5000)
  public void stringMultiThread() {
    for (int i = 0; i < 1000; i++) {
      new DefaultMuleException("customMessage");
    }
  }

}
