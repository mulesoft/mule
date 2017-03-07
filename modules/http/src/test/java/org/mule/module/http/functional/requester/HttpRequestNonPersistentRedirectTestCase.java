/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static java.util.logging.Logger.getLogger;
import static javax.servlet.http.HttpServletResponse.SC_TEMPORARY_REDIRECT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.LOCATION;

import org.mule.construct.Flow;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.glassfish.grizzly.nio.SelectorRunner;
import org.junit.Test;

public class HttpRequestNonPersistentRedirectTestCase extends AbstractHttpRequestTestCase {

  private AtomicBoolean redirect = new AtomicBoolean(true);

  @Override
  protected String getConfigFile() {
    return "http-request-non-persistent-redirect-config.xml";
  }

  @Test
  public void nonPersistentRedirect() throws Exception {
    final AtomicBoolean servereLogOutput = new AtomicBoolean();
    Logger logger = getLogger(SelectorRunner.class.getName());
    logger.addHandler(new Handler()
    {
      @Override
      public void publish(LogRecord record)
      {
        if(record.getLevel() == Level.SEVERE)
        {
          servereLogOutput.set(true);
        }
      }

      @Override
      public void flush()
      {

      }

      @Override
      public void close() throws SecurityException
      {

      }
    });

    Flow flow = (Flow) getFlowConstruct("nonPersistentRedirect");
    flow.process(getTestEvent(TEST_PAYLOAD));
    // Ensure that NPE producing SEVERE log output does not occur in HttpTransactionContext
    assertThat(servereLogOutput.get(), is(false));
  }

  @Override
  protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
  {
    if (redirect.getAndSet(false))
    {
      response.setStatus(SC_TEMPORARY_REDIRECT);
      response.setHeader(LOCATION, "http://localhost:" + httpPort.getValue());
    }
    else
    {
      super.handleRequest(baseRequest, request, response);
    }
  }

}
