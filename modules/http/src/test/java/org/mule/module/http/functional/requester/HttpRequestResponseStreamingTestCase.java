/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpRequestResponseStreamingTestCase extends FunctionalTestCase
{
  private static final int POLL_TIMEOUT = 2000;
  private static final int POLL_DELAY = 200;

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  protected static Latch latch;
  private static AtomicBoolean executed;

  private final PollingProber pollingProber = new PollingProber(POLL_TIMEOUT, POLL_DELAY);
  private Probe processorExecuted = new Probe()
  {

    @Override
    public boolean isSatisfied()
    {
      return executed.get();
    }

    @Override
    public String describeFailure()
    {
      return "Processor should have executed at this point.";
    }

  };
  private Probe processorNotExecuted = new Probe()
  {

    @Override
    public boolean isSatisfied()
    {
      return !executed.get();
    }

    @Override
    public String describeFailure()
    {
      return "Processor should not have executed at this point.";
    }

  };

  @Override
  protected String getConfigFile()
  {
    return "http-request-response-streaming-config.xml";
  }

  @Before
  public void setUp()
  {
    latch = new Latch();
    executed = new AtomicBoolean(false);
  }

  @Test
  public void executionIsExpeditedWhenStreaming() throws Exception
  {
    runFlow("streamingClient", new DefaultMuleEvent(getTestMuleMessage(), getTestEvent(TEST_MESSAGE), false));
    pollingProber.check(processorExecuted);
    latch.release();
  }

  @Test
  public void executionHangsWhenNotStreaming() throws Exception
  {
    runFlow("noStreamingClient", new DefaultMuleEvent(getTestMuleMessage(), getTestEvent(TEST_MESSAGE), false));
    pollingProber.check(processorNotExecuted);
    latch.release();
    pollingProber.check(processorExecuted);
  }

  protected static class StatusProcessor implements MessageProcessor
  {


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
      executed.set(true);
      return event;
    }

  }

  protected static class FillAndWaitInputStreamProcessor implements MessageProcessor
  {

    private static final int LISTENER_BUFFER = 8 * 1024;

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
      InputStream inputStream = new InputStream()
      {

        private int sent = 0;

        @Override
        public int read() throws IOException
        {
          if (sent <= LISTENER_BUFFER)
          {
            sent++;
            return 1;
          } else
            {
            try
            {
              latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
            }
            catch (InterruptedException e)
            {
              // Do nothing
            }
            return -1;
          }
        }
      };
      event.getMessage().setPayload(inputStream);
      return event;
    }

  }
}
