/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

public class WorkDirectoryTestCase extends FunctionalTestCase {

  private static final String TEST_FILENAME = "test.txt";


  @Override
  protected String getConfigFile() {
    return "work-directory-config-flow.xml";
  }

  @Override
  protected void doTearDown() throws Exception {
    // clean out the directory tree that's used as basis for this test
    File outputDir = getWorkingDirectory();
    assertTrue(FileUtils.deleteTree(outputDir));

    super.doTearDown();
  }

  @Test
  public void testWorkDirectory() throws Exception {
    FunctionalTestComponent ftc = (FunctionalTestComponent) getComponent("relay");
    ftc.setEventCallback(new EventCallback() {

      @Override
      public void eventReceived(MuleEventContext context, Object component) throws Exception {
        File workDir = getFileInsideWorkingDirectory("work");
        String[] filenames = workDir.list();
        assertTrue(filenames.length > 0);
        for (String filename : filenames) {
          if (filename.contains(TEST_FILENAME)) {
            return;
          }
        }

        fail("no work dir file matching filename " + TEST_FILENAME);
      }
    });

    writeTestMessageToInputDirectory();
    checkOutputDirectory();
  }

  private void writeTestMessageToInputDirectory() throws FileNotFoundException, IOException {
    File outFile = new File(getFileInsideWorkingDirectory("in"), TEST_FILENAME);
    FileOutputStream out = new FileOutputStream(outFile);
    out.write(TEST_MESSAGE.getBytes());
    out.close();
  }

  private void checkOutputDirectory() throws Exception {
    for (int i = 0; i < 30; i++) {
      File outDir = getFileInsideWorkingDirectory("out");
      if (outDir.exists()) {
        String[] filenames = outDir.list();
        if (filenames.length > 0) {
          for (String filename : filenames) {
            if (filename.contains(TEST_FILENAME)) {
              return;
            }
          }
        }
      }

      Thread.sleep(1000);
    }

    fail("no file with name " + TEST_FILENAME + " in output directory");
  }

}
