/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class HttpRequestResponseNoStreamingTestCase extends AbstractHttpRequestResponseStreamingTestCase {

  @Test
  public void executionHangsWhenNotStreaming() throws Exception
  {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      executorService.execute(new Runnable() {

                                @Override
                                public void run() {
                                  try {
                                    runFlow("noStreamingClient", getTestMuleMessage());
                                  } catch (Exception e) {
                                    // Do nothing
                                  }
                                }
                              }
      );
      pollingProber.check(processorNotExecuted);
      latch.release();
      pollingProber.check(processorExecuted);
    }
    finally
    {
      executorService.shutdown();
    }
  }

}
