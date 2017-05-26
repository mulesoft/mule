/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.mule.api.config.MuleProperties.MULE_HTTP_STREAM_RESPONSE;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestResponseStreamingTestCase extends AbstractHttpRequestResponseStreamingTestCase {

  @Rule
  public SystemProperty streamResponse = new SystemProperty(MULE_HTTP_STREAM_RESPONSE, "true");

  @Test
  public void executionIsExpeditedWhenStreaming() throws Exception
  {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try
    {
      executorService.execute(new Runnable() {

                                @Override
                                public void run() {
                                  try {
                                    runFlow("streamingClient", getTestMuleMessage());
                                  } catch (Exception e) {
                                    // Do nothing
                                  }
                                }
                              }

      );
      pollingProber.check(processorExecuted);
      latch.release();
    }
    finally
    {
      executorService.shutdown();
    }
  }

}
