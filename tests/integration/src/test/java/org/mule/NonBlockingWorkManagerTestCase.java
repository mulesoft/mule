/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.api.context.WorkManager;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class NonBlockingWorkManagerTestCase extends FunctionalTestCase
{

  private static final int MAX_THREADS = 2;
  private static final int WAIT_TIME = 1000;

  @Override
  protected String getConfigFile()
  {
    return "non-blocking-work-manager-config.xml";
  }

  @Test
  public void executesEverythingWhenDefault() throws InterruptedException
  {
    final List<String> threads = runAndCollectThreads("nonBlockingFlow");
    assertThat(threads, hasItems("nonBlockingFlow.01", "nonBlockingFlow.01"));
  }

  @Test
  public void executesOnCallerWhenRun() throws InterruptedException
  {
    final List<String> threads = runAndCollectThreads("customNonBlockingFlow");
    assertThat(threads, containsInAnyOrder("customNonBlockingFlow.01", Thread.currentThread().getName()));
  }

  private List<String> runAndCollectThreads(String flowName) throws InterruptedException
  {
    final CountDownLatch latch = new CountDownLatch(2);
    final List<String> threads = new LinkedList<>();

    WorkManager workManager = ((Flow) muleContext.getRegistry().lookupFlowConstruct(flowName)).getWorkManager();
    for (int i = 0; i < MAX_THREADS; i++)
    {
      workManager.execute(new Runnable()
      {

        @Override
        public void run()
        {
          threads.add(Thread.currentThread().getName());
          try
          {
            Thread.sleep(WAIT_TIME);
          }
          catch (InterruptedException e)
          {
            // Do nothing
          }
          latch.countDown();
        }

      });
    }

    latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
    assertThat(threads, hasSize(MAX_THREADS));
    return threads;
  }

}
