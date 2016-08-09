/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.file;

import static org.junit.Assert.fail;

import org.mule.compatibility.transport.file.ExpressionFilenameParser;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class FileExceptionStrategyFunctionalTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/providers/file/file-exception-strategy-flow.xml";
  }

  @Before
  public void before() {
    ExpressionFilenameParser.resetCount();
  }

  @Test
  public void testExceptionInTransformer() throws Exception {
    File f = FileUtils.newFile(getFileInsideWorkingDirectory("in/test.txt").getAbsolutePath());
    f.createNewFile();

    // try a couple of times with backoff strategy, then fail
    File errorFile = FileUtils.newFile(getFileInsideWorkingDirectory("errors/test-0.out").getAbsolutePath());
    boolean testSucceded = false;
    int timesTried = 0;
    while (timesTried <= 3) {
      Thread.sleep(500 * ++timesTried);
      if (errorFile.exists()) {
        testSucceded = true;
        break;
      }
    }

    if (!testSucceded) {
      fail("Exception strategy hasn't moved the file to the error folder.");
    }
  }
}
