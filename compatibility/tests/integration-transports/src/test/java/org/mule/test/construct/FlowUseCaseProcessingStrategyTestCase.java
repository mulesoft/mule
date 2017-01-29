/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.service.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.IOUtils;
import org.mule.service.http.api.HttpService;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.message.response.HttpResponse;
import org.mule.services.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

public class FlowUseCaseProcessingStrategyTestCase extends CompatibilityFunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-usecase-processing-strategy-config.xml";
  }

  @Test
  public void testHTTPStatusCodeExceptionSyncStrategy() throws MuleException, IOException, TimeoutException {
    HttpRequest httpRequest = HttpRequest.builder().setUri("http://localhost:" + dynamicPort.getNumber()).build();
    HttpResponse httpResponse = httpClient.send(httpRequest, RECEIVE_TIMEOUT, false, null);

    assertThat(httpResponse.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
  }

  @Test
  public void testFileAutoDeleteSyncStrategy() throws Exception {
    MuleClient client = muleContext.getClient();
    File tempFile = createTempFile("mule-file-test-sync-");
    client.request("vm://exception", 5000);

    assertTrue(tempFile.exists());
  }

  @Test
  public void testFileAutoDeleteAsyncStrategy() throws Exception {
    MuleClient client = muleContext.getClient();
    File tempFile = createTempFile("mule-file-test-async-");
    client.request("vm://exception", 5000);

    assertTrue(tempFile.exists());
  }

  private File createTempFile(String fileName) throws IOException {
    File directory = getWorkingDirectory();
    File file = File.createTempFile(fileName, ".txt", directory);
    file.deleteOnExit();
    FileOutputStream fos = new FileOutputStream(file);
    IOUtils.write("The quick brown fox jumps over the lazy dog", fos);
    IOUtils.closeQuietly(fos);

    return file;
  }

}


