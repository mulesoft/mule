/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.internal.listener.grizzly.BaseResponseCompletionHandler.DEFAULT_BUFFER_SIZE;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.OutputHandler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.fluent.Request;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerOutputHandlerTestCase extends FunctionalTestCase
{

  private static OutputHandler outputHandler;
  private static List<String> chunks;

  @Rule
  public DynamicPort port = new DynamicPort("port");


  @Override
  protected String getConfigFile()
  {
    return "http-listener-output-handler-config.xml";
  }

  @Before
  public void setUp()
  {
    chunks = new ArrayList<>();
  }

  @Test
  public void singleChunk() throws Exception
  {
    validateResponseFor(new OutputHandler()
    {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        // Should work with initial buffer
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 2);
        out.flush();
        out.close();
      }
    });
  }

  @Test
  public void largeChunk() throws Exception
  {
    validateResponseFor(new OutputHandler()
    {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        // Should require larger buffer
        saveAndWrite(out, DEFAULT_BUFFER_SIZE * 2);
        out.flush();
        out.close();
      }
    });
  }

  @Test
  public void multipleChunks() throws Exception
  {
    validateResponseFor(new OutputHandler() {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        // Should work with initial buffer
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 4);
        // Should work with initial buffer
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 2);
        // Should force flush
        saveAndWrite(out, DEFAULT_BUFFER_SIZE);
        // Should force flush
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 4);
        out.flush();
        out.close();
      }
    });
  }

  @Test
  public void noFlush() throws Exception
  {
    validateResponseFor(new OutputHandler() {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException {
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 4);
        out.close();
      }
    });
  }

  @Test
  public void multipleFlushes() throws Exception
  {
    validateResponseFor(new OutputHandler() {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        // Small but flush
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 4);
        out.flush();
        // Large and flush
        saveAndWrite(out, DEFAULT_BUFFER_SIZE * 2);
        out.flush();
        out.close();
      }
    });
  }

  @Test
  public void failing() throws Exception
  {
    outputHandler = new OutputHandler() {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        throw new IOException("Error");
      }
    };

    assertThat(Request.Get(getUrl()).execute().returnResponse().getStatusLine().getStatusCode(), is(500));
  }

  @Test
  public void flushAndFail() throws Exception
  {
    validateResponseFor(new OutputHandler() {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 4);
        out.flush();
        throw new IOException("Error");
      }
    });
  }

  @Test
  public void flushCloseAndFail() throws Exception
  {
    validateResponseFor(new OutputHandler() {
      @Override
      public void write(MuleEvent event, OutputStream out) throws IOException
      {
        saveAndWrite(out, DEFAULT_BUFFER_SIZE / 4);
        out.flush();
        out.close();
        throw new IOException("Error");
      }
    });
  }

  private void saveAndWrite(OutputStream out, int size) throws IOException
  {
    String chunk = RandomStringUtils.randomAlphabetic(size);
    chunks.add(chunk);
    out.write(chunk.getBytes());
  }

  private void validateResponseFor(OutputHandler payload) throws IOException {
    outputHandler = payload;
    assertThat(Request.Get(getUrl()).execute().returnContent().asString(), is(concatChunks()));
  }

  private String concatChunks()
  {
    StringBuilder builder = new StringBuilder();
    for (String chunk : chunks)
    {
      builder.append(chunk);
    }
    return builder.toString();
  }

  private String getUrl()
  {
    return String.format("http://localhost:%s/", port.getValue());
  }

  public static class OutputHandlerMessageProcessor implements MessageProcessor
  {
    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
      event.getMessage().setPayload(outputHandler);
      return event;
    }
  }

}
