/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.Method.GET;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.client.HttpClient;
import org.mule.service.http.api.client.HttpClientConfiguration;
import org.mule.service.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("See MULE-9200")
public class ValidShutdownTimeoutRequestResponseTestCase extends AbstractShutdownTimeoutRequestResponseTestCase {

  @Rule
  public SystemProperty contextShutdownTimeout = new SystemProperty("contextShutdownTimeout", "5000");

  /**
   * This client is used to hit http listeners under test.
   */
  protected HttpClient httpClient;

  @Before
  public void createHttpClient() throws RegistrationException, IOException, InitialisationException {
    httpClient = muleContext.getRegistry().lookupObject(HttpService.class).getClientFactory()
        .create(new HttpClientConfiguration.Builder().build());
    httpClient.start();
  }

  @After
  public void disposeHttpClient() {
    httpClient.stop();
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "shutdown-timeout-request-response-config.xml";
  }

  @Test
  public void testStaticComponent() throws Exception {
    doShutDownTest("staticComponentResponse", "http://localhost:" + httpPort.getNumber() + "/staticComponent");
  }

  @Test
  public void testScriptComponent() throws Exception {
    doShutDownTest("scriptComponentResponse", "http://localhost:" + httpPort.getNumber() + "/scriptComponent");
  }

  @Test
  public void testExpressionTransformer() throws Exception {
    doShutDownTest("expressionTransformerResponse", "http://localhost:" + httpPort.getNumber() + "/expressionTransformer");
  }

  private void doShutDownTest(final String payload, final String url) throws MuleException, InterruptedException {
    final boolean[] results = new boolean[] {false};

    Thread t = new Thread() {

      @Override
      public void run() {
        try {
          HttpRequest request =
              HttpRequest.builder().setUri(url).setMethod(GET).setEntity(new ByteArrayHttpEntity(payload.getBytes())).build();
          final HttpResponse response = httpClient.send(request, RECEIVE_TIMEOUT, false, null);
          results[0] = payload.equals(IOUtils.toString(((InputStreamHttpEntity) response.getEntity()).getInputStream()));
        } catch (Exception e) {
          // Ignore
        }
      }
    };
    t.start();

    // Make sure to give the request enough time to get to the waiting portion of the feed.
    waitLatch.await();

    muleContext.stop();

    t.join();

    assertTrue("Was not able to process message ", results[0]);
  }
}
