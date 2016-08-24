/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.transport.file.FileConnector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class WorkDirectoryPropertiesTestCase extends FunctionalTestCase {

  private File dataFolder;

  public WorkDirectoryPropertiesTestCase() {
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "work-directory-properties-config.xml";
  }

  @Before
  public void createDataFolder() throws Exception {
    dataFolder = new File(muleContext.getConfiguration().getWorkingDirectory(), "data");

    if (!dataFolder.exists()) {
      assertTrue("Unable to create test folder", dataFolder.mkdirs());
    }
  }

  @Test
  public void testName() throws Exception {
    File testfile = createTestFile(dataFolder, "sample.txt");

    muleContext.start();

    MuleMessage response = muleContext.getClient().request("vm://testOut", RECEIVE_TIMEOUT * 6).getRight().get();

    assertTrue(response.getPayload() instanceof Map);
    Map<String, String> payload = (Map<String, String>) response.getPayload();
    assertEquals(dataFolder.getCanonicalPath(), payload.get(FileConnector.PROPERTY_SOURCE_DIRECTORY));
    assertEquals(testfile.getName(), payload.get(FileConnector.PROPERTY_SOURCE_FILENAME));
  }

  private File createTestFile(File parentFolder, String fileName) throws IOException {
    File result = new File(parentFolder, fileName);

    FileOutputStream out = new FileOutputStream(result);
    out.write(TEST_MESSAGE.getBytes());
    out.close();

    return result;
  }
}
