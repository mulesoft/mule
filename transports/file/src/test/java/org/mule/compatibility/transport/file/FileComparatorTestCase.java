/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.transport.file.FileConnector;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class FileComparatorTestCase extends FunctionalTestCase {

  public static final String FILE_CONNECTOR_NAME = "fileConnector";
  public static final int TIMEOUT = 50000;
  public static final String FILE_NAMES[] = {"first", "second"};
  public static final String COMPONENT_NAME = "FolderTO";
  public static final String INPUT_FOLDER = "in";

  @Override
  protected String getConfigFile() {
    return "file-functional-config.xml";
  }

  @Test
  public void testComparator() throws Exception {
    final CountDownLatch countDown = new CountDownLatch(2);
    EventCallback callback = new EventCallback() {

      @Override
      public void eventReceived(MuleEventContext context, Object component) throws Exception {
        int index = (int) countDown.getCount() - 1;
        assertEquals(FILE_NAMES[index], context.getMessage().getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME));
        countDown.countDown();
      }
    };

    ((FunctionalTestComponent) getComponent(COMPONENT_NAME)).setEventCallback(callback);

    ((Connector) muleContext.getRegistry().lookupObject(FILE_CONNECTOR_NAME)).stop();
    File f1 = FileUtils.newFile(getFileInsideWorkingDirectory(INPUT_FOLDER + File.separator + FILE_NAMES[0]).getAbsolutePath());
    assertTrue(f1.createNewFile());
    Thread.sleep(1000);
    File f2 = FileUtils.newFile(getFileInsideWorkingDirectory(INPUT_FOLDER + File.separator + FILE_NAMES[1]).getAbsolutePath());
    assertTrue(f2.createNewFile());
    Thread.sleep(1000);
    ((Connector) muleContext.getRegistry().lookupObject(FILE_CONNECTOR_NAME)).start();
    assertTrue(countDown.await(TIMEOUT, TimeUnit.MILLISECONDS));
  }
}
