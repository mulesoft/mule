/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.file;

import static org.junit.Assert.assertFalse;

import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;

import org.junit.Test;

public class FileAppendEndpointTestCase extends FileAppendConnectorTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/providers/file/mule-fileappend-endpoint-config-flow.xml";
  }

  @Override
  @Test
  public void testBasic() throws Exception {
    File myDir = FileUtils.newFile(OUTPUT_DIR);

    // output directory may not exist before dispatching to the endpoint with
    // invalid
    // configuration
    File outputFile = FileUtils.newFile(myDir, OUTPUT_FILE);
    assertFalse(outputFile.exists());

    // this should throw java.lang.IllegalArgumentException: Configuring
    // 'outputAppend' on a
    // file endpoint is no longer supported. You may configure it on a file
    // connector instead.
    MuleClient client = muleContext.getClient();
    client.dispatch("vm://fileappend", "Hello1", null);

    assertFalse(outputFile.exists());
  }
}
