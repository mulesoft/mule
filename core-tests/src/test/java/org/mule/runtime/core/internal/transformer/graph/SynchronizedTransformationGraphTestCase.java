/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.transformer.graph;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.internal.transformer.ResolverException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SynchronizedTransformationGraphTestCase extends TransformationGraphTestCase {

  private static int CONCURRENCY_TEST_SIZE = 1000;
  private static int MAX_TIMEOUT_SECONDS = 20;


  @Test
  public void modifyGraphWhileResolvingTransformer() throws ResolverException, InterruptedException {
    final Converter xmlToJson = new MockConverterBuilder().from(XML_DATA_TYPE).to(JSON_DATA_TYPE).build();
    final Converter inputStreamToXml = new MockConverterBuilder().from(INPUT_STREAM_DATA_TYPE).to(XML_DATA_TYPE).build();

    final SynchronizedTransformationGraph graph = new SynchronizedTransformationGraph();
    final TransformationGraphLookupStrategy lookupStrategyTransformation = new TransformationGraphLookupStrategy(graph);

    Runnable addTransformer = new Runnable() {

      @Override
      public void run() {
        graph.addConverter(xmlToJson);
        graph.addConverter(inputStreamToXml);
      }
    };
    Runnable resolveTransformer = new Runnable() {

      @Override
      public void run() {
        lookupStrategyTransformation.lookupConverters(INPUT_STREAM_DATA_TYPE, JSON_DATA_TYPE);
      }
    };

    List<Runnable> runnables = new ArrayList<>();
    for (int i = 0; i < CONCURRENCY_TEST_SIZE; i++) {
      runnables.add(addTransformer);
      runnables.add(resolveTransformer);
    }

    assertConcurrent("Modify transformers while resolving it", runnables, MAX_TIMEOUT_SECONDS);
  }

  public static void assertConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds)
      throws InterruptedException {
    final int numThreads = runnables.size();
    final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
    final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
    try {
      final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
      final CountDownLatch afterInitBlocker = new CountDownLatch(1);
      final CountDownLatch allDone = new CountDownLatch(numThreads);
      for (final Runnable submittedTestRunnable : runnables) {
        threadPool.submit(new Runnable() {

          public void run() {
            allExecutorThreadsReady.countDown();
            try {
              afterInitBlocker.await();
              submittedTestRunnable.run();
            } catch (final Throwable e) {
              exceptions.add(e);
              e.printStackTrace();
            } finally {
              allDone.countDown();
            }
          }
        });
      }
      // wait until all threads are ready
      assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent",
                 allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
      // start all test runners
      afterInitBlocker.countDown();
      assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds",
                 allDone.await(maxTimeoutSeconds, TimeUnit.HOURS));
    } finally {
      threadPool.shutdownNow();
    }
    assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
  }

}
