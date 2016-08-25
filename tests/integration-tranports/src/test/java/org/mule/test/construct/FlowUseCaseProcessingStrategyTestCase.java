/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("MULE-9630")
public class FlowUseCaseProcessingStrategyTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-usecase-processing-strategy-config.xml";
  }

  @Test
  public void testHTTPStatusCodeExceptionSyncStrategy() throws MuleException {
    MuleClient client = muleContext.getClient();
    final HttpRequestOptions httpRequestOptions = newOptions().disableStatusCodeValidation().build();
    MuleMessage exception = client.send("http://localhost:" + dynamicPort.getNumber(),
                                        MuleMessage.builder().nullPayload().build(), httpRequestOptions)
        .getRight();
    assertThat(exception.getInboundProperty("http.status", 0), is(500));
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

    assertFalse(tempFile.exists());
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


