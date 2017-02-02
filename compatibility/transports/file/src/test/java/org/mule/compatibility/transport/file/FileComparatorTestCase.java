/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.compatibility.transport.file.FileConnector.PROPERTY_ORIGINAL_FILENAME;
import static org.mule.runtime.core.util.FileUtils.newFile;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.core.api.message.InternalMessage;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class FileComparatorTestCase extends CompatibilityFunctionalTestCase {

  public static final String FILE_CONNECTOR_NAME = "fileConnector";
  public static final int TIMEOUT = 50000;
  public static final String FILE_NAMES[] = {"first", "second"};
  public static final String COMPONENT_NAME = "FolderTO";
  public static final String INPUT_FOLDER = "in";
  public static final String ENCODING = null;

  @Override
  protected String getConfigFile() {
    return "file-functional-config.xml";
  }

  @Test
  public void testComparator() throws Exception {
    final CountDownLatch countDown = new CountDownLatch(2);
    EventCallback callback = (context, component, muleContext) -> {
      int index = (int) countDown.getCount() - 1;
      assertEquals(FILE_NAMES[index], ((InternalMessage) context.getMessage()).getInboundProperty(PROPERTY_ORIGINAL_FILENAME));
      countDown.countDown();
    };

    ((FunctionalTestComponent) getComponent(COMPONENT_NAME)).setEventCallback(callback);

    ((Connector) muleContext.getRegistry().lookupObject(FILE_CONNECTOR_NAME)).stop();
    File f1 = newFile(getFileInsideWorkingDirectory(INPUT_FOLDER + File.separator + FILE_NAMES[0]).getAbsolutePath());
    assertTrue(f1.createNewFile());
    writeStringToFile(f1, FILE_NAMES[0], ENCODING);
    Thread.sleep(1000);
    File f2 = newFile(getFileInsideWorkingDirectory(INPUT_FOLDER + File.separator + FILE_NAMES[1]).getAbsolutePath());
    assertTrue(f2.createNewFile());
    writeStringToFile(f2, FILE_NAMES[0], ENCODING);
    Thread.sleep(1000);
    ((Connector) muleContext.getRegistry().lookupObject(FILE_CONNECTOR_NAME)).start();
    assertTrue(countDown.await(TIMEOUT, TimeUnit.MILLISECONDS));
  }
}
